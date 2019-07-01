package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

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

        OsmBounds mapBounds = data.getBounds();
        List<OsmNode> roadSigns = getRoadSigns(data);

        int signCount = roadSigns.size();
        double[] signLats = new double[signCount];
        double[] signLons = new double[signCount];
        int i = 0;

        for (OsmNode roadSign : roadSigns)
        {
            signLats[i] = roadSign.getLatitude();
            signLons[i++] = roadSign.getLongitude();
        }

        eu.kickuth.mthesis.Map m = new eu.kickuth.mthesis.Map(mapBounds, signLats, signLons);
        m.writeImage();

    }

    private static OsmIterator readData()
    {
        // Open dump file as stream
        InputStream input = null;
        try
        {
            input = ClassLoader.getSystemClassLoader().getResource("./osm_data/tue.osm.pbf").openStream();
        } catch (NullPointerException e)
        {
            System.out.println("Failed to read map dump!");
            System.exit(1);
        } catch (IOException e)
        {
            System.out.println("Failed to locate map dump!");
            System.exit(1);
        }

        // Return a reader for PBF data
        return new PbfIterator(input, true);
    }

    private static List<OsmNode> getRoadSigns(InMemoryMapDataSet data)
    {
        List<OsmNode> signs = new LinkedList<>();

        for (OsmNode node : data.getNodes().valueCollection()) {
            Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);

            String trafficSign = tags.get("traffic_sign");
            if (trafficSign != null)
            {
                signs.add(node);
            }
        }

        return signs;
    }

    private static void printStuff(OsmIterator dataIterator)
    {
        // Init counters for nodes and traffic signs
        int nodeCount = 0;
        int trafficSignCount = 0;

        // Collect types and counts of traffic signs
        Map<String, Integer> sign_types = new TreeMap<>();

        // Iterate contained entities
        for (EntityContainer container : dataIterator)
        {
            nodeCount++;

            switch (container.getType())
            {
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
        for (String s : sign_types.keySet())
        {
            System.out.println(String.format("Traffic sign: %s, count: %d", s, sign_types.get(s)));
        }
        System.out.println(String.format("Number of nodes: %d, \nNumber of traffic signs: %d", nodeCount, trafficSignCount));
    }
}
