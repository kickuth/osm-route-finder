package eu.kickuth.mthesis.web;

import eu.kickuth.mthesis.*;
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
import java.util.stream.Collectors;

import static spark.Spark.*;

public class Webserver {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private final Graph graph;
    private final WayFinder solver;

    private static final VelocityEngine ve = new VelocityEngine();
    private static String poiJSONString;

    public Webserver(Graph g, WayFinder solver) {
        graph = g;
        this.solver = solver;
        Set<Node> poiNodes = graph.adjList.keySet();
        poiNodes.removeIf((node) -> StringUtils.isEmpty(node.getType()));
        GeoJSONObject poiJSON = new GeoJSONObject();
        poiJSON.addPois(poiNodes);
        poiJSONString = poiJSON.toString();
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
        // TODO temporary/debug
        {
            for (String s : req.queryParams()) {
                logger.debug(s);
            }
            logger.debug(req.queryParams("algo"));
        }
        // load map html template
        Template htmlTemplate = ve.getTemplate( "web/map.vm" );

        // create context to add data
        VelocityContext htmlContext = new VelocityContext();

        // TODO experimental code. Put in own function
        String geoJSON = "[]";
        if (StringUtils.equals(req.queryParams("algo"), "shortest path")) {
            List<Node> path = solver.shortestPath().stream()
                    .map(dNode -> dNode.node).collect(Collectors.toList());;
            List<double[]> pathPois = new LinkedList<>();
            for (Node onPath : path) {
                pathPois.add(new double[]{onPath.getLat(), onPath.getLon()});
            }
            GeoJSONObject pathJSON = new GeoJSONObject();
            pathJSON.addPath(pathPois);
            geoJSON = pathJSON.toString();
        }

        htmlContext.put("JSONString", geoJSON);
        htmlContext.put("poiString", poiJSONString);

        // render template
        StringWriter writer = new StringWriter();
        htmlTemplate.merge( htmlContext, writer );

        return writer.toString();
    }
}
