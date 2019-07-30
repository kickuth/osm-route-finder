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
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.io.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args)
    {
        InMemoryMapDataSet data = readData();

        Graph<OsmNode, DefaultWeightedEdge> osmGraph = createGraph(data);
        //createImage(data);
    }


    private static void createImage(InMemoryMapDataSet data) {

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

        MapRenderer m = new MapRenderer(mapBounds, signPOIs, wayNodesList);
        m.writeImage(true, true);
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


    private static Graph<OsmNode, DefaultWeightedEdge> createGraph(InMemoryMapDataSet data) {
        System.out.println("creating new graph from data dump!");
        Graph<OsmNode, DefaultWeightedEdge> osmGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        //osmGraph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        for (OsmWay way : data.getWays().valueCollection()) {
            if (way.getNumberOfNodes() < 15) {  // TODO hard coded heuristic filter
                continue;
            }
            OsmNode wayPoint;
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
                    if (e == null) {  // edge already exists
                        continue;
                    }
                    // TODO use a more reliable weight (including e.g. allowed speed)
                    // TODO attention: Potentially working with small numbers!
                    double dist = Math.sqrt(Math.pow(wayPoint.getLatitude() - nextWayPoint.getLatitude(), 2) +
                            Math.pow(wayPoint.getLongitude() - nextWayPoint.getLongitude(), 2));
                    osmGraph.setEdgeWeight(e, dist);

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
