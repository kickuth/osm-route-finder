package eu.kickuth.mthesis.web;

import eu.kickuth.mthesis.*;
import eu.kickuth.mthesis.solvers.NaiveSolver;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
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
import java.util.List;
import java.util.Set;

import static spark.Spark.*;

public class Webserver {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private final Graph graph;
    private final NaiveSolver solver;

    private static final VelocityEngine ve = new VelocityEngine();
    private static final GeoJSONObject poiJSON = new GeoJSONObject();

    public Webserver(Graph g, NaiveSolver solver) {
        graph = g;
        this.solver = solver;
        Set<Node> poiNodes = graph.adjList.keySet();
        poiNodes.removeIf((node) -> StringUtils.isEmpty(node.getType()));
        poiJSON.addPois(poiNodes);
        start();
    }

    public void start() {
        // initialize engine
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        // return the web page
        get("/", "application/json", this::renderMap);
        post("/", "application/json", this::renderMap);
    }

    private String renderMap(Request req, Response res) {
        // load map html template
        Template htmlTemplate = ve.getTemplate( "web/map.vm" );

        // create context to add data
        VelocityContext htmlContext = new VelocityContext();

        // TODO experimental code. Put in own function
        GeoJSONObject pathJSON = new GeoJSONObject();
        if (req.queryParams("algo_sp") != null) {
            logger.trace("Computing shortest path");
            List<Node> path = solver.shortestPath();
            pathJSON.addPath(path);
        }
        if (req.queryParams("algo_ng") != null) {
            logger.trace("Computing naive greedy path");
            List<Node> path = solver.solve();
            pathJSON.addPath(path);
        }

        htmlContext.put("pathGeoJSON", pathJSON);
        htmlContext.put("poiGeoJSON", poiJSON);

        // render template
        StringWriter writer = new StringWriter();
        htmlTemplate.merge(htmlContext, writer);

        return writer.toString();
    }
}
