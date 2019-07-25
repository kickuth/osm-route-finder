package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Main {

    public static void main(String[] args)
    {
        OsmIterator data_iterator = readData();

        //printStuff(data_iterator);

        InMemoryMapDataSet data = null;
        try {
            data = MapDataSetLoader.read(data_iterator, true, true, true);
        } catch (IOException e)
        {
            System.out.println("Failed to load data into memory!");
            System.exit(1);
        }

        List<List<double[]>> wayNodesList = new LinkedList<>();

        for (OsmWay way : data.getWays().valueCollection())
        {
            if (way.getNumberOfNodes() < 35)  // TODO hard coded heuristic filter
            {
                continue;
            }

            List<double[]> wayNodes = new LinkedList<>();
            for (int i = 0; i < way.getNumberOfNodes(); i++)
            {
                try {
                    OsmNode wayPoint = data.getNode(way.getNodeId(i));
                    double[] latLon = {wayPoint.getLatitude(), wayPoint.getLongitude()};
                    wayNodes.add(latLon);
                } catch (EntityNotFoundException e) {
                    System.out.println("Way uses non-existing node! Ignoring.");
                }
            }
            wayNodesList.add(wayNodes);
        }


        OsmBounds mapBounds = data.getBounds();
        List<OsmNode> roadSigns = getRoadSigns(data);

        List<double[]> signPOIs = new LinkedList<>();

        for (OsmNode roadSign : roadSigns) {
            double[] d = {roadSign.getLatitude(), roadSign.getLongitude()};
            signPOIs.add(d);
        }

        eu.kickuth.mthesis.Map m = new eu.kickuth.mthesis.Map(mapBounds, signPOIs, wayNodesList);
        m.writeImage(true, true);



        Graph<OsmNode, DefaultWeightedEdge> osmGraph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        for (OsmWay way : data.getWays().valueCollection()) {
            if (way.getNumberOfNodes() < 15) {  // TODO hard coded heuristic filter
                continue;
            }
            OsmNode wayPoint = null;
            try {
                wayPoint = data.getNode(way.getNodeId(0));
                osmGraph.addVertex(wayPoint);
            } catch (EntityNotFoundException e) {
                System.out.println("Way uses non-existing first node! Ignoring way.");
                continue;
            }
            for (int i = 1; i < way.getNumberOfNodes(); i++) {
                try {
                    OsmNode nextWayPoint = data.getNode(way.getNodeId(i));
                    osmGraph.addVertex(nextWayPoint);

                    // add edge and set the edge cost
                    DefaultWeightedEdge e = osmGraph.addEdge(wayPoint, nextWayPoint);
                    if (e == null) {
                        // TODO figure out why e == null; edge already exists?
                        continue;
                    }
                    // TODO use a more reliable weight (including e.g. allowed speed)
                    // TODO attention: working with small numbers!
                    double dist = Math.sqrt(Math.pow(wayPoint.getLatitude() - nextWayPoint.getLatitude(), 2) +
                            Math.pow(wayPoint.getLongitude() - nextWayPoint.getLongitude(), 2));
                    osmGraph.setEdgeWeight(e, dist);

                    wayPoint = nextWayPoint;
                } catch (EntityNotFoundException e) {
                    System.out.println("Way uses non-existing node! Ignoring node.");
                }
            }
        }
    }


    private static OsmIterator readData() {
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

        // Return a reader for PBF data
        return new PbfIterator(input, true);
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


    private static void printStuff(OsmIterator dataIterator) {
        // Init counters for nodes and traffic signs
        int nodeCount = 0;
        int trafficSignCount = 0;

        // Collect types and counts of traffic signs
        Map<String, Integer> sign_types = new TreeMap<>();

        // Iterate contained entities
        for (EntityContainer container : dataIterator) {
            nodeCount++;

            switch (container.getType()) {
                case Node:
                    // Get the node from the container
                    OsmNode node = (OsmNode) container.getEntity();

                    // Convert the node's tags to a map
                    Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);

                    // Get the value for the 'traffic_sign' key
                    String trafficSign = tags.get("traffic_sign");
                    if (trafficSign != null) {
                        trafficSignCount++;
                        int previousCount = sign_types.getOrDefault(trafficSign, 0);
                        sign_types.put(trafficSign, previousCount+1);

                        // Print traffic sign with location
//                        System.out.println(String.format("%s at %f, %f",
//                                trafficSign, node.getLatitude(), node.getLongitude()
//                        ));
                    }
                    break;
                case Way:
//                    OsmWay way = (OsmWay) container.getEntity();
//
//                    System.out.println(way.getId());
//
//                    // Print all of the way's tags
//                    for (OsmTag tag : OsmModelUtil.getTagsAsList(way))
//                    {
//                        System.out.println(tag.toString());
//                    }
                    break;
                case Relation:
                    break;
                default:
                    System.err.println("Encountered unexpected OSM entity. Ignoring!");
            }
        }

        // Print accumulated stats
        for (String s : sign_types.keySet()) {
            System.out.println(String.format("Traffic sign: %s, count: %d", s, sign_types.get(s)));
        }
        System.out.println(String.format("Number of nodes: %d, \nNumber of traffic signs: %d", nodeCount, trafficSignCount));
    }
}
