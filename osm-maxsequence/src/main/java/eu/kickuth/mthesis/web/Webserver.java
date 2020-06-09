package eu.kickuth.mthesis.web;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.solvers.GASolver;
import eu.kickuth.mthesis.solvers.SPESolver;
import eu.kickuth.mthesis.solvers.SPSolver;
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
import java.util.Set;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;
import static spark.Spark.*;

public class Webserver {

    private static final Logger logger = LogManager.getLogger(Webserver.class);

    private final Graph graph;
    private Solver currentSolver;
    private final HashMap<String, Solver> solvers = new HashMap<>(5);

    private static final VelocityEngine ve = new VelocityEngine();  // web server
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
        currentSolver = new SPESolver(defaultSource, defaultTarget, defaultMaxDistFactor, graph);
        solvers.put("ng", currentSolver);
        solvers.put("gr", new GASolver(defaultSource, defaultTarget, defaultMaxDistFactor, graph));
        solvers.put("sp", new SPSolver(defaultSource, defaultTarget, defaultMaxDistFactor, graph));

        // get POIs from nodes, filter common (later dynamically loaded) POIs
        poiJSON = GeoJSON.createPOIList(
                graph.pois.stream().filter(
                        node -> !StringUtils.isEmpty(node.type)
                                && !node.type.equals("city_limit")
                                && !node.type.equals("DE:205")
                                && !node.type.equals("DE:206")
                                && !node.type.equals("DE:274")
                ).collect(Collectors.toList())
        );
        start();
    }

    /**
     * Boot up the web-server. Will keep the thread alive and handle requests in new threads.
     */
    private void start() {
        // Configure Spark web-server
        port(PORT);
        staticFiles.location("/web/pub");
        //staticFiles.expireTime(600);  // enable file caching and set duration
        //Spark.after((request, response) -> corsHeaders.forEach(response::header));  // allow CORS

        // initialize engine
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        // setup request handlers
        get("/", "application/json", this::renderMap);
        get("/path", "application/json", this::computePath);
        get("/status", "application/json", this::getSolverProgress);
        get("/maxdist", "application/json", this::updateMaxDist);
        post("/pois", "application/json", this::getPoisInWindow);

        logger.info("Started web-server: http://[::1]:{}/", PORT);
    }

    private String getPoisInWindow(Request req, Response res) {
        String body = req.body();
        logger.trace("Requested POIs for {}", body);
        JSONObject data = new JSONObject(body);  // available fields: north, east, south, west, zoom
        double[] area = {(double) data.get("north"), (double) data.get("south"), (double) data.get("west"), (double) data.get("east")};
        return GeoJSON.createPOIList(graph.getPois(area));
    }

    private String updateMaxDist(Request req, Response res) {
        return String.format("{ \"maxdist\":%f }",
                (currentSolver.getMaxDistance() / currentSolver.getMaxDistanceFactor())
                        * Double.parseDouble(req.queryParams("newfactor")));
    }

    private String getSolverProgress(Request req, Response res) {
        return String.format("{ \"progress\":%d }", (int) (currentSolver.getStatus() * 100));
    }

    private String computePath(Request req, Response res) {
        String reqAlgo = req.queryParams("algo");
        if (solvers.containsKey(reqAlgo)) {
            currentSolver = solvers.get(reqAlgo);
            logger.debug("Current solver set to {}", currentSolver);
        } else {
            logger.warn("Ignoring requested solver/algorithm: " + reqAlgo);
        }

        // TODO check/rework code block
        String reqSource = req.queryParams("source");
        String reqTarget = req.queryParams("sink");
        String reqMaxDistance = req.queryParams("max_dist");
        if (reqSource != null && reqTarget != null && reqMaxDistance != null) {
            try {
                int newSourceId = Integer.parseInt(reqSource);
                int newTargetId = Integer.parseInt(reqTarget);
                double newRelativeMaxDistance = Double.parseDouble(reqMaxDistance);
                Node newSource = graph.getNode(newSourceId);
                Node newTarget = graph.getNode(newTargetId);
                if (newSource == null || newTarget == null) {
                    logger.error("Invalid source or sink/target requested!");
                } else {
                    currentSolver.update(newSource, newTarget, newRelativeMaxDistance);
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to convert user input to long:\nsource: '{}'\ntarget: '{}'\nmax distance: '{}'",
                        reqSource, reqTarget, reqMaxDistance);

            }
        }

        Graph.Path path = currentSolver.solve();

        Set<Node> pathPois = graph.getPoisOnPath(path);

        Map<String, String> jsonArgs = new HashMap<>();
        jsonArgs.put("pathPois", GeoJSON.createPOIList(pathPois));
        jsonArgs.put("score", String.valueOf(currentSolver.uniqueClassScore(path)));
        jsonArgs.put("uBound", String.valueOf(currentSolver.getUpperBound()));
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
