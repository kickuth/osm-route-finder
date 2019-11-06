package eu.kickuth.mthesis.graph;

import java.util.*;
import java.util.stream.Collectors;

import eu.kickuth.mthesis.graph.Graph.Path;

public class Dijkstra {

    private final Graph graph;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final List<DijkstraNode> pqueueNodes;
    private boolean[] updatedPqueueNodes;  // keep track of which nodes need resetting after dijkstra run

    public Dijkstra(Graph graph) {
        this.graph = graph;
        pqueue = new PriorityQueue<>(graph.nodes.size());
        pqueueNodes = graph.nodes.stream().map(node -> new DijkstraNode(node, Double.POSITIVE_INFINITY)).collect(Collectors.toList());
        updatedPqueueNodes = new boolean[graph.nodes.size()];  // initialize with false
    }


    /**
     * Compute the single source shortest path to all reachable nodes.
     * @param source the node to start with
     * @return map of reachable nodes to their minimal distance from the source
     */
    public Map<Node, Double> sssp(final Node source) {
        return sssp(source, Double.POSITIVE_INFINITY);
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
            if (currentMin.distanceFromSource == Double.POSITIVE_INFINITY) {
                break;
            }
            // check whether an updated node has already been processed
            if (currentMin.wasProcessed) {
                continue;
            } else {
                currentMin.wasProcessed = true;
            }
            results.put(currentMin.node, currentMin.distanceFromSource);
            for (Node neighbour : graph.adjList.get(currentMin.node.id)) {
                double alternativeDistance = currentMin.distanceFromSource + currentMin.node.getDistance(neighbour);
                // ignore nodes outside of maxDistance
                if (alternativeDistance > maxDistance) {
                    continue;
                }
                // update node, if the new path is shorter than the previous shortest
                DijkstraNode dNeighbour = pqueueNodes.get(neighbour.id);
                if (alternativeDistance < dNeighbour.distanceFromSource) {
                    updatedPqueueNodes[neighbour.id] = true;
                    dNeighbour.distanceFromSource = alternativeDistance;
                    pqueue.add(dNeighbour);
                }
            }
        }
        // TODO reset pqueueNodes and updatedPqueueNodes in a better way (i.e. only reset ones that were altered)
        pqueueNodes.forEach(dNode -> {
            dNode.distanceFromSource = Double.POSITIVE_INFINITY;
            dNode.wasProcessed = false;
        });
        updatedPqueueNodes = new boolean[updatedPqueueNodes.length];
        return results;
    }

    /**
     * Compute a single source shortest path to the closest target node
     * @param source the node to start with
     * @param targets set of target nodes
     * @return path from source to closest target
     */
    public Path shortestPath(final Node source, final Collection<Node> targets) {
        ArrayList<Node> sources = new ArrayList<>(1);
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
        List<Node> sources = new ArrayList<>(1);
        List<Node> targets = new ArrayList<>(1);
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
        DijkstraNode backtrack = null;

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();

            // check whether an updated node has already been processed
            if (currentMin.wasProcessed) {
                continue;
            } else {
                currentMin.wasProcessed = true;
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
                DijkstraNode dNeighbour = pqueueNodes.get(neighbour.id);
                if (alternativeDistance < dNeighbour.distanceFromSource) {
                    updatedPqueueNodes[neighbour.id] = true;
                    dNeighbour.distanceFromSource = alternativeDistance;
                    previousNode.put(dNeighbour, currentMin);
                    pqueue.add(dNeighbour);
                }
            }
        }

        // reconstruct the path from the target
        LinkedList<DijkstraNode> results = new LinkedList<>();
        while (backtrack != null) {
            results.add(0, new DijkstraNode(backtrack.node, backtrack.distanceFromSource));
            backtrack = previousNode.get(backtrack);
        }
        // TODO reset pqueueNodes and updatedPqueueNodes in a better way (i.e. only reset ones that were altered)
        pqueueNodes.forEach(dNode -> {
            dNode.distanceFromSource = Double.POSITIVE_INFINITY;
            dNode.wasProcessed = false;
        });
        updatedPqueueNodes = new boolean[updatedPqueueNodes.length];
        return graph.new Path(results);
    }

    /**
     * Initialise queue with single source.
     */
    private void initDijkstra(Node source) {
        pqueue.clear();
        pqueueNodes.get(source.id).distanceFromSource = 0;
        updatedPqueueNodes[source.id] = true;
        pqueue.addAll(pqueueNodes);
    }

    /**
     * Initialise queue with multiple sources.
     */
    private void initDijkstra(Collection<Node> sources) {
        pqueue.clear();
        for (Node source : sources) {
            pqueueNodes.get(source.id).distanceFromSource = 0;
        }
        pqueue.addAll(pqueueNodes);
    }
}
