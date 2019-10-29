package eu.kickuth.mthesis;

import crosby.binary.osmosis.OsmosisReader;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.utils.OSMReader;
import eu.kickuth.mthesis.utils.OSMWriter;
import eu.kickuth.mthesis.web.Webserver;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;

import static eu.kickuth.mthesis.utils.Settings.*;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);


    public static void main(String[] args) {
        logger.trace("Loading graph from file");
        OSMReader myReader = new OSMReader();
        try {
            InputStream inputStream = new FileInputStream(OSM_DUMP);
            OsmosisReader reader = new OsmosisReader(inputStream);
            reader.setSink(myReader);
            reader.run();
        } catch (FileNotFoundException e) {
            logger.fatal("Failed to load map data", e);
            System.exit(1);
        }
        Graph osmGraph = myReader.getOsmGraph();


        // TODO experimental code
        Node source = osmGraph.getNode(1409294970);
        Node target = osmGraph.getNode(251878779);
        double maxDistanceFactor = 1.25;

        // TODO export test
//        logger.trace("Exporting graph.");
//        OSMWriter exporter = new OSMWriter();
//        exporter.export(osmGraph,"~/Desktop/EXPORT.osm.pbf");

        // start interactive web visualization
        new Webserver(source, target, maxDistanceFactor, osmGraph);
    }
}
