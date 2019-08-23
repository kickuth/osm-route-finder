package eu.kickuth.mthesis.web;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;

import static spark.Spark.*;

public class Webserver {

    public static void start(String geoJSON, String poiJSON) {
//        staticFiles.location("/web");  // Static files
//        staticFiles.expireTime(600); // cache static files for ten minutes

        // get and initialize an engine
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        // load html template
        Template htmlTemplate = ve.getTemplate( "web/map.vm" );

        // create context to add data
        VelocityContext htmlContext = new VelocityContext();

        htmlContext.put("JSONString", geoJSON);
        htmlContext.put("poiString", poiJSON);

        // render template
        StringWriter writer = new StringWriter();
        htmlTemplate.merge( htmlContext, writer );

        // return the web page
        get("/", (req, res) -> writer.toString());
    }
}
