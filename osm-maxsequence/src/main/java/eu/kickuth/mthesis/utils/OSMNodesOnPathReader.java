package eu.kickuth.mthesis.utils;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import java.util.*;
import java.util.stream.Collectors;

public class OSMNodesOnPathReader implements Sink {

    private Set<Long> nodes;

    OSMNodesOnPathReader() {
        nodes = new HashSet<>();
    }

    @Override
    public void initialize(Map<String, Object> metaData) {

    }

    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer instanceof WayContainer) {
            processWay(((WayContainer) entityContainer).getEntity());

        }
    }

    /**
     * Check if a way is considered drivable and if so, remember all node IDs on it.
     * @param osmWay the currently processed way
     */
    private void processWay(Way osmWay) {
        boolean isHighway = false;  // is road drivable?

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
                    isHighway = true;
                    break;
            }
        }
        if (isHighway) {
            nodes.addAll(osmWay.getWayNodes().stream().map(WayNode::getNodeId).collect(Collectors.toList()));
        }
    }

    /**
     * Retrieve the set of node IDs that lie on drivable roads.
     * @return ID set of nodes on roads
     */
    public Set<Long> getNodes() {
        return nodes;
    }

    @Override
    public void complete() {

    }

    @Override
    public void close() {

    }
}
