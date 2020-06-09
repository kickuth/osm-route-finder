package eu.kickuth.mthesis.utils;

import crosby.binary.osmosis.OsmosisSerializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.Source;
import org.openstreetmap.osmosis.osmbinary.file.BlockOutputStream;
//import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import java.io.*;
import java.util.*;


public class OSMRoadSimplification implements Sink, Source {

    private static final Logger logger = LogManager.getLogger(OSMRoadSimplification.class);

    public OSMRoadSimplification(final File outputFile) {
        this.outputFile = outputFile;
    }


    // data writer
    private Sink sink;
    private final File outputFile;

    // these are the nodes we want to keep with a new ID counting from 0.
    private final Map<Long, Integer> junctionIDMap = new HashMap<>(100_000);
    private int mappedID = 0;

    // Because WayNodes do not get lon/lat from file, we store a map of IDs to newly created WayNodes
    private final Map<Long, WayNode> idToLatLon = new HashMap<>(1_000_000);

    // default user and date for processed OSM file
    private static final OsmUser user = new OsmUser(0, "");
    private static final Date date = new Date(0L);

    @Override
    public void initialize(Map<String, Object> metaData) {
        logger.trace("Preprocessing: Pruning bendy road segments");
        // initialise writer
        try {
//            setSink(new XmlWriter(new BufferedWriter(new FileWriter(outputFile))));  // xml writer
            setSink(new OsmosisSerializer(new BlockOutputStream(new FileOutputStream(outputFile))));  // pbf writer
        } catch (FileNotFoundException e) {
            logger.fatal("Could not find File!", e);
//        } catch (IOException e) {  // for FileWriter in XmlWriter
//            logger.fatal("Error in FileWriter", e);
        }
    }

    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer instanceof NodeContainer) {
            processNode((Node) entityContainer.getEntity());

        } else if (entityContainer instanceof WayContainer) {
            processWay((Way) entityContainer.getEntity());

        } else if (entityContainer instanceof RelationContainer) {
            // We don't process relations

        } else if (entityContainer instanceof BoundContainer) {
            sink.process(entityContainer);

        } else {
            logger.warn("Unknown Entity: {}", entityContainer.getEntity());
        }
    }

    private void processNode(Node osmNode) {

        long nodeID = osmNode.getId();

        /*
        Because WayNodes do not get lon/lat from file (hence are always 0), we store a map of IDs to WayNodes here
         */
        idToLatLon.put(nodeID, new WayNode(nodeID, osmNode.getLatitude(), osmNode.getLongitude()));

        boolean keep = false;
        for (Tag tag : osmNode.getTags()) {
            String key = tag.getKey();
            if ("traffic_sign".equals(key) || "is_junction".equals(key)) {
                keep = true;
                break;
            }
        }

        if (keep) {
            // store old ID to node mapping, to compute distances in ways
            Collection<Tag> nodeTags = osmNode.getTags();
            nodeTags.removeIf(tag -> tag.getKey().equals("is_junction"));
            osmNode = new Node(new CommonEntityData(mappedID, 1, date, user, 0,
                    nodeTags), osmNode.getLatitude(), osmNode.getLongitude()
            );
            junctionIDMap.put(nodeID, mappedID++);


            sink.process(new NodeContainer(osmNode));
        }
    }

    private void processWay(Way osmWay) {
        WayNode currentNode = null;
        Integer junctionID;
        List<WayNode> keepers = new ArrayList<>();
        ListIterator<WayNode> wayNodes = osmWay.getWayNodes().listIterator();
        // get the first junction/sign node (and hence discard anything before it)
        while (wayNodes.hasNext()) {
            currentNode = idToLatLon.get(wayNodes.next().getNodeId());
            if ((junctionID = junctionIDMap.get(currentNode.getNodeId())) != null) {
                keepers.add(new WayNode(junctionID, currentNode.getLatitude(), currentNode.getLongitude()));
                break;
            }
        }

        double nextNodeDistanceSum = 0;
        StringBuilder distanceList = new StringBuilder();
        // now go through all other nodes, computing nodes we want to keep and distances between them
        while (wayNodes.hasNext()) {
            WayNode nextNode = idToLatLon.get(wayNodes.next().getNodeId());
            nextNodeDistanceSum += distance(currentNode, nextNode);

            // are we at a junction or road sign?
            if ((junctionID = junctionIDMap.get(nextNode.getNodeId())) != null) {
                distanceList.append(nextNodeDistanceSum).append(";");
                nextNodeDistanceSum = 0;
                keepers.add(new WayNode(junctionID, nextNode.getLatitude(), nextNode.getLongitude()));
            }

            currentNode = nextNode;
        }

        if (keepers.size() <= 1) {
            logger.info("Way {} does not contain more than one sign or junction. Ignoring.", osmWay.getId());
            return;
        }

        Collection<Tag> tags = osmWay.getTags();
        tags.add(new Tag("distance_list", distanceList.toString()));

        sink.process(new WayContainer(new Way(
                osmWay.getId(), 1, date, user, 0, tags, keepers
        )));

    }

    /**
     * This method is called, once the input file has been completely read.
     */
    @Override
    public void complete() {
        sink.complete();  // write remaining output buffer to file
    }

    @Override
    public void close() {

    }


    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }


    /**
     * Computes the distance between two lon/lat pairs.
     *
     * Adapted version from
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
     *
     * @return the distance in meters
     */
    private static double distance(WayNode fst, WayNode snd) {

        double lat1 = fst.getLatitude();
        double lon1 = fst.getLongitude();
        double lat2 = snd.getLatitude();
        double lon2 = snd.getLongitude();

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // converted to meters
    }
}