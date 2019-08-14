package eu.kickuth.mthesis;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class Dijkstra {

    private final Graph graph;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final Map<Node, DijkstraNode> lookup;

    public Dijkstra(Graph graph) {
        this.graph = graph;
        pqueue = new PriorityQueue<>(graph.adjList.size());
        lookup = new HashMap<>(graph.adjList.size());
    }


    /**
     * Compute the single source shortest path to all reachable nodes.
     * @param source the node to start with
     * @return map of reachable nodes to their minimal distance from the source
     */
    public Map<Node, Double> sssp(final Node source) {
        return sssp(source, Double.MAX_VALUE);
    }

    /**
     * Compute the single source shortest path to all nodes within the maxDistance.
     * @param source the node to start with
     * @param maxDistance maximal distance from source; nodes with larger distance are ignored
     * @return map of reachable nodes within maxDistance to their minimal distance from the source.
     */
    public Map<Node, Double> sssp(final Node source, final double maxDistance) {

        initDijkstra(source);

        Map<Node, Double> results = new HashMap<>();

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();
            // check if only unreachable nodes are left and we are done
            if (currentMin.distanceFromSource == Double.MAX_VALUE) {
                break;
            }
            // check whether an updated node has already been processed
            if (lookup.get(currentMin.node) != currentMin) {
                continue;
            }
            results.put(currentMin.node, currentMin.distanceFromSource);
            for (Node neighbour : graph.adjList.get(currentMin.node)) {
                double alternativeDistance = currentMin.distanceFromSource + currentMin.node.getDistance(neighbour);
                // ignore nodes outside of maxDistance
                if (alternativeDistance > maxDistance) {
                    continue;
                }
                // update node, if the new path is shorter than the previous shortest
                if (alternativeDistance < lookup.get(neighbour).distanceFromSource) {
                    DijkstraNode updatedNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueue.add(updatedNeighbour);
                    lookup.put(neighbour, updatedNeighbour);
                }
            }
        }
        return results;
    }

    /**
     * Compute a single source shortest path to the closest target node
     * @param source the node to start with
     * @param targets set of target nodes
     * @return path from source to closest target
     */
    public List<DijkstraNode> shortestPath(final Node source, final Set<Node> targets) {
        Set<Node> sources = new HashSet<>();
        sources.add(source);
        return shortestPath(sources, targets);

    }

    /**
     * Compute the shortest s-t-path
     * @param source the source node
     * @param target the target node
     * @return list of nodes from source to target, empty list if no path exists
     */
    public List<DijkstraNode> shortestPath(final Node source, final Node target) {
        Set<Node> sources = new HashSet<>();
        Set<Node> targets = new HashSet<>();
        sources.add(source);
        targets.add(target);
        return shortestPath(sources, targets);
    }

    /**
     * Compute the shortest path from any source to any target node
     * @param sources the set of source nodes
     * @param targets the set of target nodes
     * @return list of nodes for the shortest s-t-path, empty list if no path exists
     */
    public List<DijkstraNode> shortestPath(final Set<Node> sources, final Set<Node> targets) {
        initDijkstra(sources);
        // map to backtrack shortest path
        Map<DijkstraNode, DijkstraNode> previousNode = new HashMap<>();

        // the first reached node in targets
        DijkstraNode backtrack;

        // main loop
        while (true) {
            // if the queue is empty, we didn't find the target
            if (pqueue.isEmpty()) {
                return new LinkedList<>();
            }
            DijkstraNode currentMin = pqueue.poll();
            // check whether an updated node has already been processed
            if (lookup.get(currentMin.node) != currentMin) {
                continue;
            }
            // are we at the target yet?
            if (targets.contains(currentMin.node)) {
                backtrack = currentMin;
                break;
            }
            // check whether only unreachable nodes are left --> Target unreachable
            if (currentMin.distanceFromSource == Double.MAX_VALUE) {
                return new LinkedList<>();
            }
            for (Node neighbour : graph.adjList.get(currentMin.node)) {
                double alternativeDistance = currentMin.distanceFromSource + currentMin.node.getDistance(neighbour);
                // update node, if the new path is shorter than the previous shortest
                if (alternativeDistance < lookup.get(neighbour).distanceFromSource) {
                    DijkstraNode updatedNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueue.add(updatedNeighbour);
                    lookup.put(neighbour, updatedNeighbour);
                    previousNode.put(updatedNeighbour, currentMin);
                }
            }
        }

        // reconstruct the path from the target
        // TODO use LinkedHashMap to also store each distance?
        List<DijkstraNode> results = new LinkedList<>();
        while (backtrack != null) {
            results.add(0, backtrack);
            backtrack = previousNode.get(backtrack);
        }
        return results;
    }

    /**
     * Initialise queue with single source. See <code>initDijkstra(Set<Node> sources)</code>.
     */
    private void initDijkstra(Node source) {
        Set<Node> sources = new HashSet<>();
        sources.add(source);
        initDijkstra(sources);
    }

    /**
     * Initialise queue with multiple sources and initialise lookup table
     */
    private void initDijkstra(Set<Node> sources) {
        pqueue.clear();
        for (Node node : graph.adjList.keySet()) {
            DijkstraNode dNode;
            if (sources.contains(node)) {
                dNode = new DijkstraNode(node, 0);
                pqueue.add(dNode);
            } else {
                dNode = new DijkstraNode(node, Double.MAX_VALUE);
            }
            lookup.put(node, dNode);
        }
    }
}
