package eu.kickuth.mthesis.web;

import eu.kickuth.mthesis.*;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.solvers.GreedySolver;
import eu.kickuth.mthesis.solvers.Solver;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import spark.Request;
import spark.Response;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static spark.Spark.*;

public class Webserver {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private final Graph graph;
    private final Solver solver;
    private final Solver greedySolver;

    private static final VelocityEngine ve = new VelocityEngine();
    private static final GeoJSONObject poiJSON = new GeoJSONObject();

    public Webserver(Graph g, Solver solver) {
        graph = g;
        this.solver = solver;
        greedySolver = new GreedySolver(solver.getSource(), solver.getTarget(), solver.getMaxDistance(), g);
        Set<Node> poiNodes = graph.adjList.keySet();
        poiNodes.removeIf((node) -> StringUtils.isEmpty(node.getType()));
        poiJSON.addPois(poiNodes);
        start(4567);
    }

    public void start(int port) {
        logger.trace("Starting web-server on port {}", port);
        // Configure Spark
        port(port);
        staticFiles.location("/web/pub");
        staticFiles.expireTime(600);

        // initialize engine
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        // return the web page
        get("/", "application/json", this::renderMap);
        get("/path", "application/json", this::computePath);
        get("/status", "application/json", this::computeProgress);
        //post("/", "application/json", this::renderMap);
    }

    private String computeProgress(Request req, Response res) {
        return String.format("{ \"progress\":%d }", (int) (solver.getStatus() * 100));
    }

    private String computePath(Request req, Response res) {
        // TODO experimental code. Put in own function
        String reqSource = req.queryParams("source");
        String reqTarget = req.queryParams("sink");
        String reqMaxDistance = req.queryParams("max_dist");
        if (reqSource != null && reqTarget != null && reqMaxDistance != null) {
            try {
                long newSourceId = Long.parseLong(reqSource);
                long newTargetId = Long.parseLong(reqTarget);
                long newMaxDistance = (long) (Double.parseDouble(reqMaxDistance) * 1000);  // convert from km to m
                logger.debug("Setting source to {}, sink/target to {}, maxDistance to {}",
                        newSourceId, newTargetId, newMaxDistance);
                solver.setMaxDistance(newMaxDistance);
                Node newSource = graph.getNode(newSourceId);
                Node newTarget = graph.getNode(newTargetId);
                if (newSource == null || newTarget == null) {
                    logger.error("Invalid source or sink/target requested!");
                } else {
                    solver.setSource(newSource);
                    solver.setTarget(newTarget);
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to convert user input to string:\nsource: '{}'\ntarget: '{}'\nmax distance: '{}'",
                        reqSource, reqTarget, reqMaxDistance);

            }
        }


        GeoJSONObject pathJSON = new GeoJSONObject();
        List<Node> path;
        String reqAlgo = req.queryParams("algo");
        switch (reqAlgo) {
            case "ng":
                logger.debug("Computing naive path");
                path = solver.solve();
                pathJSON.addPath(path);
                break;
            case "gr":
                logger.debug("Computing greedy path");
                path = greedySolver.solve();
                pathJSON.addPath(path);
                break;
            default:
                logger.debug("Ignoring requested algorithm: " + reqAlgo);
        }
        return pathJSON.toString();
    }

    private String renderMap(Request req, Response res) {
        logger.trace("Accessed map from {} using {}", req.ip(), req.userAgent());
        // load map html template
        Template htmlTemplate = ve.getTemplate( "web/map.vm" );

        // create context to add data
        VelocityContext htmlContext = new VelocityContext();


        // populate html template fields
        htmlContext.put("poiGeoJSON", poiJSON);
        htmlContext.put("solver", solver);


        // render template
        StringWriter writer = new StringWriter();
        htmlTemplate.merge(htmlContext, writer);

        return writer.toString();
    }
}
