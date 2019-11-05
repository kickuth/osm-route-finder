package eu.kickuth.mthesis.graph;

import java.util.*;
import eu.kickuth.mthesis.graph.Graph.Path;

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
            for (Node neighbour : graph.adjList.get(currentMin.node.id)) {
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
    public Path shortestPath(final Node source, final Collection<Node> targets) {
        Set<Node> sources = new HashSet<>();
        sources.add(source);
        return shortestPath(sources, targets);

    }

    /**
     * Compute the shortest s-t-path
     * @param source the source node
     * @param target the target node
     * @return Path of nodes from source to target, empty Path if no path exists
     */
    public Path shortestPath(final Node source, final Node target) {
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
     * @return Path of nodes for the shortest s-t-path, empty Path if no path exists
     */
    public Path shortestPath(final Collection<Node> sources, final Collection<Node> targets) {
        initDijkstra(sources);
        // map to backtrack shortest path
        Map<DijkstraNode, DijkstraNode> previousNode = new HashMap<>();

        // the first reached node in targets
        DijkstraNode backtrack;

        // main loop
        while (true) {
            // if the queue is empty, we didn't find the target
            if (pqueue.isEmpty()) {
                return graph.new Path();
            }
            DijkstraNode currentMin = pqueue.poll();
            // check whether an updated node has already been processed
            if (!lookup.get(currentMin.node).equals(currentMin)) {
                continue;
            }
            // are we at the target yet?
            if (targets.contains(currentMin.node)) {
                backtrack = currentMin;
                break;
            }
            // get and potentially update all neighbours
            for (Node neighbour : graph.adjList.get(currentMin.node.id)) {
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
        LinkedList<DijkstraNode> results = new LinkedList<>();
        while (backtrack != null) {
            results.add(0, backtrack);
            backtrack = previousNode.get(backtrack);
        }
        return graph.new Path(results);
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
    private void initDijkstra(Collection<Node> sources) {
        pqueue.clear();
        for (Node node : graph.nodes) {
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
