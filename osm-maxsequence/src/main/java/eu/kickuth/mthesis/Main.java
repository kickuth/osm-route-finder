package eu.kickuth.mthesis;

import crosby.binary.osmosis.OsmosisReader;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.utils.OSMPreprocessor;
import eu.kickuth.mthesis.utils.OSMReader;
import eu.kickuth.mthesis.web.Webserver;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;

import static eu.kickuth.mthesis.utils.Settings.*;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);


    public static void main(String... args) {
        // preprocessing
        if (FORCE_PREPROCESS || !OSM_DUMP_PROCESSED.exists()) {
            logger.trace("Preprocessing file dump");
            try {
                InputStream osmInput = new FileInputStream(OSM_DUMP);
                OsmosisReader reader = new OsmosisReader(osmInput);
                reader.setSink(new OSMPreprocessor());
                reader.run();
            } catch (IOException e) {
                logger.fatal("Failed to preprocess map data", e);
                System.exit(1);
            }
        }

        System.exit(0);

        // import graph
        logger.trace("Loading graph from preprocessed file");
        OSMReader myReader = new OSMReader();
        try {
            InputStream inputStream = new FileInputStream(OSM_DUMP_PROCESSED);
            OsmosisReader reader = new OsmosisReader(inputStream);
            reader.setSink(myReader);
            reader.run();
        } catch (FileNotFoundException e) {
            logger.fatal("Failed to load map data", e);
            System.exit(1);
        }

        // retrieve graph from importer
        Graph osmGraph = myReader.getOsmGraph();

        // output some graph stats
        logger.info("Node count: {}", osmGraph.adjList.size());
        logger.info("POI count: {}", osmGraph.pois.size());
        logger.info("Types of POIs: {}", osmGraph.poiTypes.size());

        StringBuilder poiClassesDebug = new StringBuilder();
        osmGraph.poiTypes.forEach((key, value) -> poiClassesDebug.append("\n").append(key).append(" - ").append(value));
        logger.trace("POI classes with counts: {}", poiClassesDebug);


        // set initial values
        double maxDistanceFactor = 1.25;
        int sourceID = 27182;
        int targetID = 31415;

        Node source = osmGraph.getNode(sourceID);
        Node target = osmGraph.getNode(targetID);
        if (source == null || target == null) {
            logger.fatal("cold not retrieve default source/target nodes (IDs {}, {}) from graph.", sourceID, targetID);
            System.exit(1);
        }

        // start interactive web visualization
        new Webserver(source, target, maxDistanceFactor, osmGraph);
    }
}
