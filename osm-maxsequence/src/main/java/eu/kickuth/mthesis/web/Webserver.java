package eu.kickuth.mthesis.web;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.solvers.GreedySolver;
import eu.kickuth.mthesis.solvers.NaiveSolver;
import eu.kickuth.mthesis.solvers.Solver;
import eu.kickuth.mthesis.utils.GeoJSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;
import static spark.Spark.*;

public class Webserver {

    private static final Logger logger = LogManager.getLogger(Webserver.class);

    private final Graph graph;
    private Solver currentSolver;

    private final HashMap<String, Solver> solvers = new HashMap<>(5);
    private static final VelocityEngine ve = new VelocityEngine();
    private final String poiJSON;

    // uncomment here (and in start()) for Cross-Origin Resource Sharing
//    private static final HashMap<String, String> corsHeaders = new HashMap<>();
//    static {
//        corsHeaders.put("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
//        corsHeaders.put("Access-Control-Allow-Origin", "*");
//        corsHeaders.put("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
//        corsHeaders.put("Access-Control-Allow-Credentials", "true");
//    }

    public Webserver(Node defaultSource, Node defaultTarget, double defaultMaxDistFactor, Graph g) {
        logger.trace("Initialising solvers");
        graph = g;
        currentSolver = new NaiveSolver(defaultSource, defaultTarget, defaultMaxDistFactor, g);
        solvers.put("ng", currentSolver);
        solvers.put("gr", new GreedySolver(defaultSource, defaultTarget, defaultMaxDistFactor, g));

        // get POIs from nodes
        poiJSON = GeoJSON.createPOIList(
                graph.adjList.keySet().stream().filter(
                        (node) -> !StringUtils.isEmpty(node.getType())
                ).collect(Collectors.toList())
        );
        start();
    }

    private void start() {
        logger.trace("Starting web-server: http://[::1]:{}/", PORT);

        // Configure Spark
        port(PORT);
        staticFiles.location("/web/pub");
        //staticFiles.expireTime(600);  // cache
        //Spark.after((request, response) -> corsHeaders.forEach(response::header));  // allow CORS

        // initialize engine
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();


        // setup request handlers
        get("/", "application/json", this::renderMap);
        //post("/", "application/json", this::renderMap);
        get("/path", "application/json", this::computePath);
        get("/status", "application/json", this::computeProgress);
        get("/maxdist", "application/json", this::updateMaxDist);
        post("/pois", "application/json", this::getPoisInWindow);
    }

    private String getPoisInWindow(Request req, Response res) {
        String body = req.body();
        logger.trace("Requested POIs for {}", body);
        JSONObject data = new JSONObject(body);
        // TODO implement;
        System.out.println(data.get("south"));  // north, east, south, west, zoom
        return "";
    }

    private String updateMaxDist(Request req, Response res) {
        return String.format("{ \"maxdist\":%f }",
                (currentSolver.getMaxDistance() / currentSolver.getMaxDistanceFactor())
                        * Double.parseDouble(req.queryParams("newfactor")));
    }

    private String computeProgress(Request req, Response res) {
        return String.format("{ \"progress\":%d }", (int) (currentSolver.getStatus() * 100));
    }

    private String computePath(Request req, Response res) {
        String reqAlgo = req.queryParams("algo");
        if (solvers.containsKey(reqAlgo)) {
            logger.debug("Current solver set to: " + reqAlgo);
            currentSolver = solvers.get(reqAlgo);
        } else {
            logger.warn("Ignoring requested solver/algorithm: " + reqAlgo);
        }

        // TODO check/rework code block
        String reqSource = req.queryParams("source");
        String reqTarget = req.queryParams("sink");
        String reqMaxDistance = req.queryParams("max_dist");
        if (reqSource != null && reqTarget != null && reqMaxDistance != null) {
            try {
                long newSourceId = Long.parseLong(reqSource);
                long newTargetId = Long.parseLong(reqTarget);
                double newRelativeMaxDistance = Double.parseDouble(reqMaxDistance);
                logger.debug("Setting source to {}, sink/target to {}, relativeMaxDistance to {}",
                        newSourceId, newTargetId, newRelativeMaxDistance);
                Node newSource = graph.getNode(newSourceId);
                Node newTarget = graph.getNode(newTargetId);
                if (newSource == null || newTarget == null) {
                    logger.error("Invalid source or sink/target requested!");
                } else {
                    currentSolver.setSource(newSource);
                    currentSolver.setTarget(newTarget);
                    currentSolver.setMaxDistanceFactor(newRelativeMaxDistance);
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to convert user input to long:\nsource: '{}'\ntarget: '{}'\nmax distance: '{}'",
                        reqSource, reqTarget, reqMaxDistance);

            }
        }

        Graph.Path path = currentSolver.solve();
        String score = String.valueOf(currentSolver.uniqueClassScore(path));
        logger.info("Unique class score for {}: {}", currentSolver.getName(), score);

        Map<String, String> jsonArgs = new HashMap<>();
        jsonArgs.put("score", score);
        jsonArgs.put("shortestpathdist", String.valueOf(
                currentSolver.getMaxDistance()/currentSolver.getMaxDistanceFactor()));

        return GeoJSON.createPath(path, jsonArgs);
    }

    private String renderMap(Request req, Response res) {
        logger.trace("Accessed map from {} using {}", req.ip(), req.userAgent());
        // load map html template
        Template htmlTemplate = ve.getTemplate( "web/map.vm" );

        // create context to add data
        VelocityContext htmlContext = new VelocityContext();

        // populate html template fields
        htmlContext.put("poiGeoJSON", poiJSON);
        htmlContext.put("solver", currentSolver);

        // render template
        StringWriter writer = new StringWriter();
        htmlTemplate.merge(htmlContext, writer);

        return writer.toString();
    }
}
