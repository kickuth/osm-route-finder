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


    public static void main(String[] args) {
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
        Graph osmGraph = myReader.getOsmGraph();
        logger.debug("Node count: {}", osmGraph.adjList.size());
        logger.debug("POI count: {}", osmGraph.pois.size());
        logger.debug("Types of POIs: {}", osmGraph.poiTypes.size());
        for (var entry : osmGraph.poiTypes.entrySet()) {  // TODO experimental
            if (entry.getValue() > 10) {
                System.out.println(entry.getKey() + " -- " + entry.getValue());
            }
        }


        // TODO experimental code
        Node source = osmGraph.getNode(27182);
        Node target = osmGraph.getNode(31415);
        double maxDistanceFactor = 1.25;


        // start interactive web visualization
        new Webserver(source, target, maxDistanceFactor, osmGraph);
    }
}
