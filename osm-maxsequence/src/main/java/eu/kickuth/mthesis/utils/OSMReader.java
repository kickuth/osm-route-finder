package eu.kickuth.mthesis.utils;

import eu.kickuth.mthesis.graph.Graph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.util.*;


public class OSMReader implements Sink {

    private static final Logger logger = LogManager.getLogger(OSMReader.class);

    private HashMap<Long, eu.kickuth.mthesis.graph.Node> nodes = new HashMap<>();


    private Graph osmGraph;


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

        } else if (entityContainer instanceof BoundContainer) {
            Bound b = ((BoundContainer) entityContainer).getEntity();
            osmGraph = new Graph(new double[]{b.getTop(), b.getBottom(), b.getLeft(), b.getRight()});
            logger.info("Map bounds are: {}", b);

        } else {
            logger.warn("Unknown Entity: {}", entityContainer.getEntity());
        }
    }

    private void processNode(Node osmNode) {
        String type = null;
        for (Tag tag : osmNode.getTags()) {
            if ("traffic_sign".equalsIgnoreCase(tag.getKey())) {
                type = tag.getValue().intern();
                break;
            }
            // TODO add fake classes? Other type sources?
        }
        var newNode = new eu.kickuth.mthesis.graph.Node(
                osmNode.getId(), osmNode.getLatitude(), osmNode.getLongitude(), type);
        nodes.put(osmNode.getId(), newNode);
    }

    private void processWay(Way osmWay) {
        boolean isHighway = false;  // is road drivable?
        boolean isOneWay = false;
        String rt = null;  // what type of road is it?

        // filter for useful roads and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "access":  // can we access the road?
                    if ("no".equalsIgnoreCase(wayTag.getValue()) || "private".equalsIgnoreCase(wayTag.getValue())) {
                        return;
                    }
                    break;
                case "highway":  // is it a (probably) drivable road?
                    rt = wayTag.getValue();
                    if (!( rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential") )) {
                        return;
                    }
                    isHighway = true;
                    break;
                case "junction":  // is it a roundabout (implies one directional)?
                    if ("roundabout".equalsIgnoreCase(wayTag.getValue())) {
                        isOneWay = true;
                    }
                    break;
                case "oneway":  // is it explicitly one directional?
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
        var currentNode = nodes.get(wn.getNodeId());
        currentNode.setRoadType(rt);
        osmGraph.addNode(currentNode);
        eu.kickuth.mthesis.graph.Node nextNode;
        while (wayNodes.hasNext()) {
            wn = wayNodes.next();
            nextNode = nodes.get(wn.getNodeId());
            nextNode.setRoadType(rt);
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
        // TODO
        // postprocess: Remove nodes without neighbours and dead ends
        int deadEndCount = 0;
        for (var e : osmGraph.adjList.entrySet()) {
            if (e.getValue().isEmpty()) {
                deadEndCount++;
            }
        }
        if (deadEndCount > 0) {
            logger.warn("DEAD ENDS IN GRAPH: " + deadEndCount);
        }
    }

    @Override
    public void close() {

    }

    public Graph getOsmGraph() {
        return osmGraph;
    }
}
