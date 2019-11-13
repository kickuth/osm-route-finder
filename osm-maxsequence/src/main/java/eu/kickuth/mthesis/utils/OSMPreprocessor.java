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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;

public class OSMPreprocessor implements Sink, Source {

    private static final Logger logger = LogManager.getLogger(OSMPreprocessor.class);
    private static final OsmUser user = new OsmUser(0, "");
    private static final Date date = new Date();

    private Sink sink;

    private final Set<Long> nodesOnRoads;

    private final Map<Long, Integer> idMap;
    private int mappedID = 0;

    public OSMPreprocessor() throws FileNotFoundException {
        logger.trace("First run: Finding node IDs on paths");
        OSMNodesOnPathReader nodesReader = new OSMNodesOnPathReader();
        InputStream inputStream = new FileInputStream(OSM_DUMP);
        OsmosisReader reader = new OsmosisReader(inputStream);
        reader.setSink(nodesReader);
        reader.run();
        nodesOnRoads = nodesReader.getNodes();
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
            setSink(new OsmosisSerializer(new BlockOutputStream(new FileOutputStream(OSM_DUMP_PROCESSED))));
        } catch (FileNotFoundException e) {
            logger.fatal("Could not find File!", e);
        }

    }

    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer instanceof NodeContainer) {
            processNode(((NodeContainer) entityContainer).getEntity());

        } else if (entityContainer instanceof WayContainer) {
            processWay(((WayContainer) entityContainer).getEntity());

        } else if (entityContainer instanceof RelationContainer) {
            // We don't process relations

        } else if (entityContainer instanceof BoundContainer) {
            sink.process(entityContainer);

        } else {
            logger.warn("Unknown Entity: {}", entityContainer.getEntity());
        }
    }

    private void processNode(Node osmNode) {
        if (!nodesOnRoads.remove(osmNode.getId())) {
            // continue if id was not present
            // TODO why is this true after preprocessed? why OSMNodesOnPathReader: #nodes n!= #nodeIDs?
            return;
        }
        idMap.put(osmNode.getId(), mappedID);
        Collection<Tag> tags = new ArrayList<>(1);
        for (Tag tag : osmNode.getTags()) {
            if ("traffic_sign".equalsIgnoreCase(tag.getKey())) {
                Pattern roadSignPattern = Pattern.compile("(DE:\\d+)|(city_limit)");
                Matcher matcher = roadSignPattern.matcher(tag.getValue());
                if (matcher.find()) {
                    tags.add(new Tag("traffic_sign", matcher.group(0)));
                }
                while (matcher.find()) {
                    // TODO do not ignore? extend regex?
                    logger.info("Ignoring additional sign: {}", matcher.group(0));
                }
            }
        }
        Node node = new Node(mappedID++, 1, date, user, 0, tags, osmNode.getLatitude(), osmNode.getLongitude());
        sink.process(new NodeContainer(node));
    }

    private void processWay(Way osmWay) {
        boolean isHighway = false;  // is road drivable?
        boolean isOneWay = false;
        Collection<Tag> tags = new ArrayList<>(2);

        // filter for useful roads and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "access":  // can we access the road?
                    if ("no".equalsIgnoreCase(wayTag.getValue()) || "private".equalsIgnoreCase(wayTag.getValue())) {
                        return;
                    }
                    break;
                case "highway":  // is it a (probably) drivable road?
                    String rt = wayTag.getValue();
                    if (!(rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential"))) {
                        return;
                    }
                    tags.add(new Tag("highway", rt));
                    isHighway = true;
                    break;
                case "junction":  // is it a roundabout (implies one directional)?
                    if ("roundabout".equalsIgnoreCase(wayTag.getValue()) && !isOneWay) {
                        isOneWay = true;
                        tags.add(new Tag("oneway", "yes"));
                    }
                    break;
                case "oneway":  // is it explicitly one directional?
                    if ("yes".equalsIgnoreCase(wayTag.getValue()) && !isOneWay) {
                        isOneWay = true;
                        tags.add(new Tag("oneway", "yes"));
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
                n -> new WayNode(idMap.get(n.getNodeId()), n.getLatitude(), n.getLongitude())
        ).collect(Collectors.toList());

        Way way = new Way(osmWay.getId(), 1, date, user, 0, tags, newWayNodes);
        sink.process(new WayContainer(way));

    }

    @Override
    public void complete() {

    }

    @Override
    public void close() {

    }
}
