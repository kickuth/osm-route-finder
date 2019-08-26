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
        logger.info("running Dijkstra experiments");
        int maxDistance = 125_000; // in meters
        // pick a random source and target node
        Iterator<Node> iter = osmGraph.adjList.keySet().iterator();
        //Random rand = new Random();
        //int nodeCount = osmGraph.adjList.size();
        int sourceIdx = 1200; //rand.nextInt(nodeCount);
        int targetIdx = 9001; //rand.nextInt(nodeCount);
        Node source = null;
        Node target = null;
        for (int i = 0; i < Math.max(sourceIdx, targetIdx) + 1; i++) {
            if (i == sourceIdx) {
                source = iter.next();
            } else if (i == targetIdx) {
                target = iter.next();
            } else {
                iter.next();
            }
        }


        // initialise solver
        NaiveSolver naiveSolver = new NaiveSolver(osmGraph.clone(), source, target, maxDistance);

        // compute shortest path and its score
        List<Node> shortestPath = naiveSolver.shortestPath();  // simply beautiful syntax.
        int shortestPathScore = naiveSolver.uniqueClassScore(shortestPath);
        logger.info("Unique class score for shortest path: " + shortestPathScore);

        // run naive greedy approach
        List<Node> naiveGreedyPath = naiveSolver.solve();
        int naiveGreedyPathScore = naiveSolver.uniqueClassScore(naiveGreedyPath);
        logger.info("Unique class score for naive greedy path: " + naiveGreedyPathScore);

        // start interactive web visualization
        new Webserver(osmGraph, naiveSolver);
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
        int nodeCount = data.getNodes().size();
        Graph osmGraph = new Graph(nodeCount);

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
                    wayPointType = wayTags.get("traffic_sign");
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

        // add road signs that are next to roads ( O(n^2)! )
//        int addedRoadsigns = 0; // TODO temp
//        for (OsmNode roadSign : data.getNodes().valueCollection()) {
//            Map<String, String> tags = OsmModelUtil.getTagsAsMap(roadSign);
//
//            String trafficSign = tags.get("traffic_sign");
//            if (StringUtils.isEmpty(trafficSign)) {
//                continue;
//            }
//            Node roadNode = new Node(roadSign.getId(), roadSign.getLatitude(), roadSign.getLongitude(), trafficSign);
//            // check if sign already is part of the graph
//            if (osmGraph.adjList.containsKey(roadNode)) {
//                continue;
//            }
//            List<Node> candidates = new LinkedList<>();
//            for (Node node : osmGraph.adjList.keySet()) {
//                if (Math.abs(node.getLat() - roadSign.getLatitude()) < 0.001 &&
//                        Math.abs(node.getLon() - roadSign.getLongitude()) < 0.0006) {
//                    /* TODO hard coded direct lon/lat comparison
//                    TODO Length in meters of 1° of latitude = always 111.32 km
//                    TODO Length in meters of 1° of longitude = 40075 km * cos( latitude ) / 360
//                    */
//                    candidates.add(node);
//                }
//            }
//            if (candidates.isEmpty()) {
//                // no close nodes present in graph
//                continue;
//            }
//            double currentMin = Double.MAX_VALUE;
//            Node closestNode = null;
//            for (Node node : candidates) {
//                double dist = roadNode.getDistance(node);
//                if (dist < currentMin) {
//                    currentMin = dist;
//                    closestNode = node;
//                }
//            }
//            addedRoadsigns++;
//            closestNode.setType(roadNode.getType());
//        }
//        System.out.println("With a lot of effort added roadsigns: " + addedRoadsigns);


        return osmGraph;
    }
}
