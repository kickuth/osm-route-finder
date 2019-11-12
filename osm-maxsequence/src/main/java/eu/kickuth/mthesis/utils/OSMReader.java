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
        int id = -1;
        try {
            id = Math.toIntExact(osmNode.getId());
        } catch (ArithmeticException e) {
            logger.error("ID is too large to fit in int!", e);
        }

        osmGraph.addNode(new eu.kickuth.mthesis.graph.Node(
                id, osmNode.getLatitude(), osmNode.getLongitude(), type));
    }

    private void processWay(Way osmWay) {
        boolean isOneWay = false;
        String roadType = null;  // what type of road is it?

        // get road tag and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "highway":  // is it a (probably) drivable road?
                    roadType = wayTag.getValue();
                    break;
                case "oneway":  // is it explicitly one directional?
                    isOneWay = "yes".equalsIgnoreCase(wayTag.getValue());
                    break;
                default:
                    // ignore all other tags
                    // TODO include maxspeed? (see also preprocessing)
            }
        }

        if (roadType == null) {
            logger.error("OSM dump was not properly preprocessed (way is not a highway)! Ignoring way.");
            return;
        }

        // iterate through all way nodes and add them to the graph
        ListIterator<WayNode> wayNodes = osmWay.getWayNodes().listIterator();
        WayNode wn = wayNodes.next();
        var currentNode = osmGraph.getNode(Math.toIntExact(wn.getNodeId()));
        currentNode.setRoadType(roadType);
        eu.kickuth.mthesis.graph.Node nextNode;
        while (wayNodes.hasNext()) {
            wn = wayNodes.next();
            nextNode = osmGraph.getNode(Math.toIntExact(wn.getNodeId()));
            nextNode.setRoadType(roadType);
            osmGraph.addEdge(currentNode, nextNode);
            if (!isOneWay) {
                osmGraph.addEdge(nextNode, currentNode);
            }
            currentNode = nextNode;
        }

    }

    @Override
    public void complete() {
        // TODO postprocess: Remove nodes without neighbours and dead ends?
        int deadEndCount = 0;
        for (var e : osmGraph.adjList) {
            if (e.isEmpty()) {
                deadEndCount++;
            }
        }
        if (deadEndCount > 0) {
            logger.debug("Graph contains {} dead ends.", deadEndCount);
        }
    }

    @Override
    public void close() {

    }

    public Graph getOsmGraph() {
        return osmGraph;
    }
}
