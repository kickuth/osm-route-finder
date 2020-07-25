package eu.kickuth.mthesis.utils;

import eu.kickuth.mthesis.graph.Edge;
import eu.kickuth.mthesis.graph.Graph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.util.*;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.LIMIT_FAKE_CLASSES;


public class OSMReader implements Sink {

    private static final Logger logger = LogManager.getLogger(OSMReader.class);

    private Graph osmGraph;  // the resulting graph after import

    // since our fake classes are sampled, the order of the classes does not correlate with the number of their
    // occurrences. They are also written to file in order. Hence, once we encounter a sign we no longer want to
    // process (FC number > LIMIT_FAKE_CLASSES), we can ignore all further signs all together.
    private boolean acceptFurtherSigns = true;  // only import road signs while this is true

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
        // set traffic sign and add to graph
        if (acceptFurtherSigns) {
            for (Tag tag : osmNode.getTags()) {
                if ("traffic_sign".equals(tag.getKey())) {
                    String sign = tag.getValue();

                    // stop reading signs if we reach our predefined limit
                    if (LIMIT_FAKE_CLASSES >= 0 && sign.startsWith("FC ")) {
                        int currentClassNumber = Integer.parseInt(sign.substring(3));
                        if (currentClassNumber > LIMIT_FAKE_CLASSES) {
                            acceptFurtherSigns = false;
                            break;
                        }
                    }

                    type = sign.intern();
                    break;
                }
            }
        }
        int id;
        try {
            id = Math.toIntExact(osmNode.getId());
        } catch (ArithmeticException e) {
            logger.error("ID is too large to fit in int! Ignoring node!", e);
            return;
        }
        osmGraph.addNode(new eu.kickuth.mthesis.graph.Node(
                id, osmNode.getLatitude(), osmNode.getLongitude(), type));
    }

    private void processWay(Way osmWay) {
        boolean isOneWay = false;
        String roadType = null;  // what type of road is it?
        ArrayList<Double> distanceList = new ArrayList<>();

        // get road tag and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "highway":  // is it a (probably) drivable road?
                    roadType = wayTag.getValue();
                    break;
                case "oneway":  // is it explicitly one directional?
                    isOneWay = "yes".equalsIgnoreCase(wayTag.getValue());
                    break;
                case "distance_list":  // is this a pruned way, with distances given rather than computed using lat/lon?
                    distanceList = Arrays.stream(wayTag.getValue().split(";")).map(Double::parseDouble).collect(Collectors.toCollection(ArrayList::new));
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

        ListIterator<Double> distanceListIterator= distanceList.listIterator();

        // iterate through all way nodes and add them to the graph
        ListIterator<WayNode> wayNodes = osmWay.getWayNodes().listIterator();
        WayNode wn = wayNodes.next();
        var currentNode = osmGraph.getNode(Math.toIntExact(wn.getNodeId()));
//        currentNode.setRoadType(roadType);
        eu.kickuth.mthesis.graph.Node nextNode;
        while (wayNodes.hasNext()) {
            wn = wayNodes.next();
            nextNode = osmGraph.getNode(Math.toIntExact(wn.getNodeId()));
//            nextNode.setRoadType(roadType);

            if (distanceListIterator.hasNext()) {
                double nextDistance = distanceListIterator.next();
                osmGraph.addEdge(new Edge(currentNode, nextNode, nextDistance));
                if (!isOneWay) {
                    osmGraph.addEdge(new Edge(nextNode, currentNode, nextDistance));
                }
            } else {
                osmGraph.addEdge(new Edge(currentNode, nextNode));
                if (!isOneWay) {
                    osmGraph.addEdge(new Edge(nextNode, currentNode));
                }
            }
            currentNode = nextNode;
        }
    }

    /**
     * This method is called, once the input file has been completely read.
     */
    @Override
    public void complete() {
        // TODO postprocess: Remove nodes without neighbours and dead ends?
        int deadEndCount = 0;
        for (var e : osmGraph.adjList) {
            if (e.isEmpty()) {
                deadEndCount++;
            }
        }
        logger.debug("Graph contains {} dead ends.", deadEndCount);
    }

    @Override
    public void close() {

    }

    public Graph getOsmGraph() {
        return osmGraph;
    }
}