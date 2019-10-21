package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import eu.kickuth.mthesis.solvers.NaiveSolver;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.solvers.Solver;
import eu.kickuth.mthesis.web.Webserver;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.trace("Starting application.");
        final InMemoryMapDataSet data = readData();
        final Graph osmGraph = createGraph(data);


        // TODO experimental code
        Node source = osmGraph.getNode(1409294970);
        Node target = osmGraph.getNode(251878779);
        int maxDistance = 150_000; // in meters

        // start interactive web visualization
        new Webserver(source, target, maxDistance, osmGraph);
    }

    private static InMemoryMapDataSet readData() {
        // Open dump file as stream
        logger.debug("reading data dump");
        InputStream input = null;
        try {
            File f = new File("src/main/resources/osm_data/tue.osm.pbf");
            input = new FileInputStream(f);
        } catch (IOException e) {
            logger.fatal("Failed to locate map dump!");
            System.exit(1);
        }

        // reader for PBF data
        OsmIterator data_iterator = new PbfIterator(input, true);

        try {
            // return InMemoryMapDataSet
            return MapDataSetLoader.read(data_iterator, true, true, false);
        } catch (IOException e)
        {
            logger.fatal("Failed to load data into memory!");
            System.exit(1);
        }

        return null;
    }


    private static Graph createGraph(InMemoryMapDataSet data) {
        logger.trace("creating graph from data dump");
        Graph osmGraph = new Graph(data.getNodes().size());

        for (OsmWay way : data.getWays().valueCollection()) {

            // filter for useful roads
            Map<String, String> wayTags = OsmModelUtil.getTagsAsMap(way);
            String rt = wayTags.get("highway");
            String access = wayTags.get("access");
            String area = wayTags.get("area");
            if (rt == null ||  // not a road
                    (area != null && area.equals("yes")) || // way describes an area and not a road
                    (access != null && access.equals("no")) ||  // not accessible
                    // filter for roads allowing motorised vehicles
                    !( rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential") )
            ) {
                continue;
            }

            // check if the road is one way only (i.e. we shouldn't add back edges later)
            String oneWayTag = wayTags.get("oneway");
            boolean oneWay = false;
            if (oneWayTag != null && oneWayTag.equals("yes")) {
                oneWay = true;
            }

            Node wayPoint;
            Map<String, String> nodeTags;
            try {
                // add the first node to the graph
                OsmNode wpt = data.getNode(way.getNodeId(0));
                nodeTags = OsmModelUtil.getTagsAsMap(wpt);
                String wayPointType = nodeTags.get("traffic_sign");
                if (StringUtils.isEmpty(wayPointType)) {
                    // get way class
                    wayPointType = wayTags.get("traffic_sign");
                    // assign some fake classes
                    Random r = new Random(wpt.getId());
                    if (r.nextDouble() < 0.01) {
                        wayPointType = "FC " + (char) (65 + r.nextInt(20));
                    }
                    // TODO max speed, put in own method?
                }
                wayPoint = new Node(wpt.getId(), wpt.getLatitude(), wpt.getLongitude(), wayPointType);
                osmGraph.addNode(wayPoint);
            } catch (EntityNotFoundException e) {
                logger.warn("Way uses non-existing first node! Ignoring way.");
                continue;
            }
            for (int i = 1; i < way.getNumberOfNodes(); i++) {
                try {
                    // add the next node to the graph
                    OsmNode nextWpt = data.getNode(way.getNodeId(i));
                    nodeTags = OsmModelUtil.getTagsAsMap(nextWpt);
                    String wayPointType = nodeTags.get("traffic_sign");
                    if (StringUtils.isEmpty(wayPointType)) {
                        wayPointType = wayTags.get("traffic_sign");
                        // TODO max speed / see first node wayPointType
                    }
                    Node nextWayPoint = new Node(nextWpt.getId(), nextWpt.getLatitude(), nextWpt.getLongitude(), wayPointType);
                    osmGraph.addNode(nextWayPoint);

                    // add edge to the graph
                    osmGraph.addEdge(wayPoint, nextWayPoint);
                    if (!oneWay) {
                        osmGraph.addEdge(nextWayPoint, wayPoint);
                    }

                    wayPoint = nextWayPoint;
                } catch (EntityNotFoundException e) {
                    logger.warn("Way uses non-existing node! Ignoring node.");
                }
            }
        }

        return osmGraph;
    }
}
