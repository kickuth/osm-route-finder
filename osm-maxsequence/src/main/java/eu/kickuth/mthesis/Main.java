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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        InMemoryMapDataSet data = readData();

        Graph osmGraph = createGraph(data);
        OsmBounds mapBounds = data.getBounds();
        List<OsmNode> roadSigns = getRoadSigns(data);

        List<double[]> signPOIs = new LinkedList<>();
        for (OsmNode roadSign : roadSigns) {
            double[] d = {roadSign.getLatitude(), roadSign.getLongitude()};
            signPOIs.add(d);
        }

        MapRenderer m = new MapRenderer(mapBounds, signPOIs, osmGraph);
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


    private static Graph createGraph(InMemoryMapDataSet data) {
        System.out.println("creating new graph from data dump.");
        int nodeCount = data.getNodes().size();
        Graph osmGraph = new Graph(nodeCount);

        int idIncrement = 0;
        for (OsmWay way : data.getWays().valueCollection()) {

            // filter for useful roads
            Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
            String rt = tags.get("highway");
            String access = tags.get("access");
            if (rt == null || (access != null && access.equals("no")) ||
                    !( rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential") )) {
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
                wayPoint = new Node(idIncrement++, wpt.getLatitude(), wpt.getLongitude(), ""); // TODO wpt.getId()
                osmGraph.addNode(wayPoint);
            } catch (EntityNotFoundException e) {
                System.out.println("Way uses non-existing first node! Ignoring way.");
                continue;
            }
            for (int i = 1; i < way.getNumberOfNodes(); i++) {
                try {
                    // add the next node to the graph
                    OsmNode nextWpt = data.getNode(way.getNodeId(i));
                    //TODO nextWpt.getId()
                    Node nextWayPoint = new Node(idIncrement++, nextWpt.getLatitude(), nextWpt.getLongitude(), "");
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
