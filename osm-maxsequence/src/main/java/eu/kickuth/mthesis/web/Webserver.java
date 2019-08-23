package eu.kickuth.mthesis.web;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import spark.Request;
import spark.Response;

import java.io.StringWriter;

import static spark.Spark.*;

public class Webserver {

    private static final VelocityEngine ve = new VelocityEngine();
    private static String geoJSON;
    private static String poiJSON;

    public static void start(String geoJSON, String poiJSON) {
        // TODO refactor
        Webserver.geoJSON = geoJSON;
        Webserver.poiJSON = poiJSON;
//        staticFiles.location("/web");  // Static files
//        staticFiles.expireTime(600); // cache static files for ten minutes

        // initialize engine
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        // return the web page
        get("/", Webserver::renderMap);
        post("/", Webserver::renderMap);
    }

    private static String renderMap(Request req, Response res) {
        // load map html template
        Template htmlTemplate = ve.getTemplate( "web/map.vm" );

        // create context to add data
        VelocityContext htmlContext = new VelocityContext();

        htmlContext.put("JSONString", geoJSON);
        htmlContext.put("poiString", poiJSON);

        // render template
        StringWriter writer = new StringWriter();
        htmlTemplate.merge( htmlContext, writer );

        return writer.toString();
    }
}
