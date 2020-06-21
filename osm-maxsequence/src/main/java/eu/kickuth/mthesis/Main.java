package eu.kickuth.mthesis;

import crosby.binary.osmosis.OsmosisReader;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.utils.OSMPreprocessor;
import eu.kickuth.mthesis.utils.OSMReader;
import eu.kickuth.mthesis.utils.OSMRoadSimplification;
import eu.kickuth.mthesis.web.Webserver;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.*;

import static eu.kickuth.mthesis.utils.Settings.*;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);


    public static void main(String... args) {
        preprocess();

        Graph osmGraph = loadGraph();

        // output some graph stats
        logger.info("Node count: {}", osmGraph.adjList.size());
        logger.info("POI count: {}", osmGraph.pois.size());
        logger.info("Types of POIs: {}", osmGraph.poiClassesToCount.size());

        StringBuilder poiClassesDebug = new StringBuilder();
        osmGraph.poiClassesToCount.forEach((key, value) -> poiClassesDebug.append("\n").append(key).append(" - ").append(value));
        logger.trace("POI classes with counts: {}", poiClassesDebug);


        // set initial values
        double maxDistanceFactor = 1.25;
        int sourceID = 27182;
        int targetID = 31415;

        Node source = osmGraph.getNode(sourceID);
        Node target = osmGraph.getNode(targetID);
        if (source == null || target == null) {
            logger.fatal("could not retrieve default source/target nodes (IDs {}, {}) from graph.", sourceID, targetID);
            System.exit(1);
        }

        // start interactive web visualization
        new Webserver(source, target, maxDistanceFactor, osmGraph);
    }

    public static void preprocess() {
        if (FORCE_PREPROCESS || !OSM_DUMP_PROCESSED.exists()) {
            logger.trace("Preprocessing OSM file");
            try {
                final File temporary_dump = new File(OSM_DUMP.getPath() + "_TEMP");
                processData(new OSMPreprocessor(temporary_dump), OSM_DUMP);
                processData(new OSMRoadSimplification(OSM_DUMP_PROCESSED), temporary_dump);
            } catch (FileNotFoundException e) {
                logger.error("Failed to preprocess data!", e);
            } finally {
                if (!OSM_DUMP_PROCESSED.exists()) {
                    logger.fatal("No preprocessed data present; exiting!");
                    System.exit(1);
                }
            }
        }
    }

    public static Graph loadGraph() {
        logger.trace("Loading graph from preprocessed file");
        OSMReader graphReader = new OSMReader();
        processData(graphReader, OSM_DUMP_PROCESSED);
        return graphReader.getOsmGraph();
    }

    /**
     * Read binary OSM data using Osmosis
     * @param sink OSM data processor
     * @param dataFile Binary file
     */
    private static <T extends Sink> void processData(T sink, File dataFile) {
        logger.trace("Reading File {}", dataFile);
        try {
            OsmosisReader reader = new OsmosisReader(new FileInputStream(dataFile));
            reader.setSink(sink);
            reader.run();
        } catch (FileNotFoundException e) {
            logger.fatal("Failed to process OSM data file!", e);
            System.exit(1);
        }
    }
}
