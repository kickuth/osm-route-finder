package eu.kickuth.mthesis.utils;

import crosby.binary.osmosis.OsmosisReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;

public class OSMPreprocessor implements Sink, Source {

    private static final Logger logger = LogManager.getLogger(OSMPreprocessor.class);

    // default user and date for processed OSM file
    private static final OsmUser user = new OsmUser(0, "");
    private static final Date date = new Date(0L);

    // repeatable random numbers for fake classes
    private final Random random = new Random(12345);

    // data writer
    private Sink sink;

    private final Set<Long> nodesOnRoads;
    private final Set<Long> nodesOnJunctions;

    private final Map<Long, Integer> idMap;
    private int mappedID = 0;

    private final File outputFile;

    public OSMPreprocessor(final File outputFile) throws FileNotFoundException {
        logger.trace("First run: Finding node IDs on paths");
        this.outputFile = outputFile;
        OSMNodesOnPathReader nodesReader = new OSMNodesOnPathReader();
        InputStream inputStream = new FileInputStream(OSM_DUMP);
        OsmosisReader reader = new OsmosisReader(inputStream);
        reader.setSink(nodesReader);
        reader.run();
        nodesOnRoads = nodesReader.getNodeIDs();
        nodesOnJunctions = nodesReader.getJunctionIDs();
        idMap = new HashMap<>((nodesOnRoads.size() * 4) / 3);
    }

    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    @Override
    public void initialize(Map<String, Object> metaData) {
        logger.trace("Second run: Preprocessing");
        // initialise writer
        try {
//            setSink(new XmlWriter(new BufferedWriter(new FileWriter(outputFile))));  // xml writer
            setSink(new OsmosisSerializer(new BlockOutputStream(new FileOutputStream(outputFile))));  // pbf writer
        } catch (FileNotFoundException e) {
            logger.fatal("Could not find File!", e);
//        } catch (IOException e) {  // for FileWriter in XmlWriter
//            logger.fatal("Error using FileWriter!", e);
        }
    }

    @Override
    public void process(EntityContainer container) {
        if (container instanceof NodeContainer) {
            processNode((Node) container.getEntity());

        } else if (container instanceof WayContainer) {
            processWay((Way) container.getEntity());

        } else if (container instanceof RelationContainer) {
            // We don't process relations

        } else if (container instanceof BoundContainer) {
            sink.process(container);

        } else {
            logger.warn("Unknown Entity: {}", container.getEntity());
        }
    }

    private void processNode(Node osmNode) {
        Long nodeID = osmNode.getId();
        if (!nodesOnRoads.contains(nodeID)) {
            // continue if id was not present
            return;
        }
        idMap.put(nodeID, mappedID);
        Collection<Tag> tags = new ArrayList<>(1);
        for (Tag tag : osmNode.getTags()) {
            if ("traffic_sign".equalsIgnoreCase(tag.getKey())) {
                Pattern roadSignPattern = Pattern.compile("(DE:\\d+)|(city_limit)");
//                Pattern roadSignPattern = Pattern.compile("(DE:\\d+)");
                Matcher matcher = roadSignPattern.matcher(tag.getValue());
                if (matcher.find()) {
                    String type = matcher.group(0);
                    // replace some city limits with fake classes for diversity
                    if (type.equals("city_limit")) {
                        // get a random number, between 0 and 90
                        int randomInt = random.nextInt(91);
                        if (randomInt >= 65) {  // A=65 to Z=90
                            type = String.valueOf((char) randomInt);
                        }
                    }
                    tags.add(new Tag("traffic_sign", type));
                }
                while (matcher.find()) {
                    // TODO do not ignore? extend regex?
                    logger.info("Ignoring additional sign: {}", matcher.group(0));
                }
            }
        }
        if (nodesOnJunctions.contains(nodeID)) {
            tags.add(new Tag("is_junction", "yes"));
        }

        sink.process(new NodeContainer(new Node(
                new CommonEntityData(mappedID++, 1, date, user, 0, tags),
                osmNode.getLatitude(), osmNode.getLongitude()
        )));
    }

    private void processWay(Way osmWay) {
        boolean isHighway = false;  // is road drivable?
        boolean isOneWay = false;
        Collection<Tag> newTags = new ArrayList<>();

        // filter for useful roads and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "highway":  // is it a (probably) drivable road?
                    String rt = wayTag.getValue();
                    if (!(rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential"))) {
                        return;
                    }
                    newTags.add(new Tag("highway", rt));
                    isHighway = true;
                    break;
                case "oneway":  // is it explicitly one directional?
                    if ("yes".equalsIgnoreCase(wayTag.getValue()) && !isOneWay) {
                        isOneWay = true;
                        newTags.add(new Tag("oneway", "yes"));
                    }
                    break;
                case "junction":  // is it a roundabout (implies one directional)?
                    if ("roundabout".equalsIgnoreCase(wayTag.getValue()) && !isOneWay) {
                        isOneWay = true;
                        newTags.add(new Tag("oneway", "yes"));
                    }
                    break;
                case "access":  // can we access the road?
                    if ("no".equalsIgnoreCase(wayTag.getValue()) || "private".equalsIgnoreCase(wayTag.getValue())) {
                        return;
                    }
                    break;
                default:
                    // ignore all other tags
                    // TODO include maxspeed?
            }
        }
        if (!isHighway) {
            // way is not a road
            return;
        }

        List<WayNode> newWayNodes = osmWay.getWayNodes().stream().map(
                // TODO note that the library only stores WayNode IDs, not lon/lat...
                n -> new WayNode(idMap.get(n.getNodeId()), n.getLatitude(), n.getLongitude())
        ).collect(Collectors.toList());

        Way newWay = new Way(new CommonEntityData(osmWay.getId(), 1, date, user, 0, newTags), newWayNodes);

        sink.process(new WayContainer(newWay));
    }

    /**
     * This method is called, once the input file has been completely read.
     */
    @Override
    public void complete() {
        sink.complete();  // write out remaining output buffer
    }

    @Override
    public void close() {

    }
}