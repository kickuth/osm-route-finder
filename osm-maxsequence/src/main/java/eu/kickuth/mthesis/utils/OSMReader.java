package eu.kickuth.mthesis.utils;

import crosby.binary.osmosis.OsmosisReader;
import eu.kickuth.mthesis.Main;
import eu.kickuth.mthesis.graph.Graph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

import static eu.kickuth.mthesis.utils.Settings.*;


public class OSMReader implements Sink {

    private static final Logger logger = LogManager.getLogger(OSMReader.class);

    private HashMap<Long, String> nodeTypes = new HashMap<>();


    private Graph osmGraph = new Graph();


    @Override
    public void initialize(Map<String, Object> metaData) {

    }

    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer instanceof NodeContainer) {
            processNode(((NodeContainer) entityContainer).getEntity());

        } else if (entityContainer instanceof WayContainer) {
            processWay(((WayContainer) entityContainer).getEntity());

        } else if (entityContainer instanceof RelationContainer) {
            // We don't process relations

        } else {
            logger.warn("Unknown Entity: {}", entityContainer.getEntity());
        }
    }

    private void processNode(Node osmNode) {
        for (Tag tag : osmNode.getTags()) {
            if ("traffic_sign".equalsIgnoreCase(tag.getKey())) {
                nodeTypes.put(osmNode.getId(), tag.getValue());
                break;
            }
        }
    }

    private void processWay(Way osmWay) {
        boolean isHighway = false;
        boolean isOneWay = false;

        // filter for useful roads and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "access":  // can we access the road?
                    if ("no".equalsIgnoreCase(wayTag.getValue()) || "private".equalsIgnoreCase(wayTag.getValue())) {
                        return;
                    }
                    break;
//                TODO ignore areas?
//                see also https://wiki.openstreetmap.org/wiki/Key:area
//                case "area":
//                    if ("yes".equalsIgnoreCase(wayTag.getValue())) {
//                        return;
//                    }
//                    break;
                case "highway":
                    String rt = wayTag.getValue();
                    if (!( rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential") )) {
                        return;
                    }
                    isHighway = true;
                    break;
                case "oneway":
                    isOneWay = "yes".equalsIgnoreCase(wayTag.getValue());
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

        // iterate through all way nodes and add them to the graph
        ListIterator<WayNode> wayNodes = osmWay.getWayNodes().listIterator();
        WayNode wn = wayNodes.next();
        eu.kickuth.mthesis.graph.Node currentNode = new eu.kickuth.mthesis.graph.Node(
                wn.getNodeId(),wn.getLatitude(), wn.getLongitude(), nodeTypes.get(wn.getNodeId())
        );
        osmGraph.addNode(currentNode);
        eu.kickuth.mthesis.graph.Node nextNode;
        while (wayNodes.hasNext()) {
            wn = wayNodes.next();
            nextNode = new eu.kickuth.mthesis.graph.Node(
                    wn.getNodeId(),wn.getLatitude(), wn.getLongitude(), nodeTypes.get(wn.getNodeId())
            );
            osmGraph.addNode(nextNode);
            osmGraph.addEdge(currentNode, nextNode);
            if (!isOneWay) {
                osmGraph.addEdge(nextNode, currentNode);
            }
            currentNode = nextNode;
        }

    }

    @Override
    public void complete() {
        Main.osmGraph = osmGraph;
    }

    @Override
    public void close() {

    }
}
