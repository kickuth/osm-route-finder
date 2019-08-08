package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        InMemoryMapDataSet data = readData();

        Graph osmGraph = createGraph(data);
        OsmBounds mapBounds = data.getBounds();
        List<OsmNode> roadSigns = getRoadSigns(data);


        MapRenderer mapExport = new MapRenderer(mapBounds, osmGraph);
        //mapExport.writeImage(true, true);

        List<double[]> signPois = new LinkedList<>();
        for (OsmNode roadSign : roadSigns) {
            double[] d = {roadSign.getLatitude(), roadSign.getLongitude()};
            signPois.add(d);
        }
        mapExport.addPOISet(signPois);


        // TODO experimental code
        System.out.println("running Dijkstra experiments");
        Iterator<Node> iter = osmGraph.adjList.keySet().iterator();
        Random rand = new Random();
        int sourceIdx = rand.nextInt(osmGraph.adjList.size());
        int targetIdx = rand.nextInt(osmGraph.adjList.size());

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

        Dijkstra dTest = new Dijkstra(osmGraph, source);
        double maxDistance = 0.15;
        Map<Node, Double> reachableSet = dTest.sssp(maxDistance);
        System.out.println("Reachable nodes with maxDistance " + maxDistance + ": " + reachableSet.size());

        List<Node> shortestPath = dTest.sssp(target);
        System.out.println("Shortest path Node count (!= length): " + shortestPath.size());

        List<double[]> resultPOIs = new LinkedList<>();
        resultPOIs.add(new double[] {source.getLat(), source.getLon()});
        resultPOIs.add(new double[] {target.getLat(), target.getLon()});
        //for (Node reachable : reachableSet.keySet()) {
        for (Node onPath : shortestPath) {
            resultPOIs.add(new double[] {onPath.getLat(), onPath.getLon()});
        }
        mapExport.addPOISet(resultPOIs);
        String fileLoc = "/home/todd/Desktop/maps/random-st-path.png";
        mapExport.writeImage(true, true, fileLoc);
    }

    private static InMemoryMapDataSet readData() {
        // Open dump file as stream
        InputStream input = null;
        try {
            input = ClassLoader.getSystemClassLoader().getResource("./osm_data/tue.osm.pbf").openStream();
        } catch (NullPointerException e) {
            System.out.println("Failed to read map dump!");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Failed to locate map dump!");
            System.exit(1);
        }

        // reader for PBF data
        OsmIterator data_iterator = new PbfIterator(input, true);

        try {
            // return InMemoryMapDataSet
            return MapDataSetLoader.read(data_iterator, true, true, true);
        } catch (IOException e)
        {
            System.out.println("Failed to load data into memory!");
            System.exit(1);
        }

        return null;
    }


    private static Graph createGraph(InMemoryMapDataSet data) {
        System.out.println("creating graph from data dump.");
        int nodeCount = data.getNodes().size();
        Graph osmGraph = new Graph(nodeCount);

        // TODO int idIncrement = 0;
        for (OsmWay way : data.getWays().valueCollection()) {

            // filter for useful roads
            Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
            String rt = tags.get("highway");
            String access = tags.get("access");
            String area = tags.get("area");
            if (rt == null ||  // not a road
                    (area != null && area.equals("yes")) || // way describes an area and not a road
                    (access != null && access.equals("no")) ||  // not accessible
                    // filter for roads with motorised vehicles
                    !( rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential") )
            ) {
                continue;
            }

            // check if the road is one way only (i.e. we shouldn't add back edges later)
            String oneWayTag = tags.get("oneway");
            boolean oneWay = false;
            if (oneWayTag != null && oneWayTag.equals("yes")) {
                oneWay = true;
            }

            Node wayPoint;
            try {
                // add the first node to the graph
                OsmNode wpt = data.getNode(way.getNodeId(0));
                wayPoint = new Node(wpt.getId(), wpt.getLatitude(), wpt.getLongitude(), ""); // TODO idIncrement++
                osmGraph.addNode(wayPoint);
            } catch (EntityNotFoundException e) {
                System.out.println("Way uses non-existing first node! Ignoring way.");
                continue;
            }
            for (int i = 1; i < way.getNumberOfNodes(); i++) {
                try {
                    // add the next node to the graph
                    OsmNode nextWpt = data.getNode(way.getNodeId(i));
                    Node nextWayPoint = new Node(nextWpt.getId(), nextWpt.getLatitude(), nextWpt.getLongitude(), ""); // TODO idIncrement++
                    osmGraph.addNode(nextWayPoint);

                    // add edge to the graph
                    osmGraph.addEdge(wayPoint, nextWayPoint);
                    if (!oneWay) {
                        osmGraph.addEdge(nextWayPoint, wayPoint);
                    }

                    wayPoint = nextWayPoint;
                } catch (EntityNotFoundException e) {
                    System.out.println("Way uses non-existing node! Ignoring node.");
                }
            }
        }
        return osmGraph;
    }


    private static List<OsmNode> getRoadSigns(InMemoryMapDataSet data) {
        List<OsmNode> signs = new LinkedList<>();

        for (OsmNode node : data.getNodes().valueCollection()) {
            Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);

            String trafficSign = tags.get("traffic_sign");
            if (trafficSign != null) {
                signs.add(node);
            }
        }

        return signs;
    }
}
