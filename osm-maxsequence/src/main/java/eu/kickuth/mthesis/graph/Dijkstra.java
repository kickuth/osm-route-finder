package eu.kickuth.mthesis.graph;

import java.util.*;
import java.util.stream.Collectors;

import eu.kickuth.mthesis.graph.Graph.Path;

public class Dijkstra {

    private final Graph graph;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final List<DijkstraNode> pqueueNodes;
    private List<Integer> updatedPqueueNodes;  // keep track of which nodes need resetting after dijkstra run

    public Dijkstra(Graph graph) {
        this.graph = graph;
        pqueue = new PriorityQueue<>(graph.nodes.size());
        pqueueNodes = graph.nodes.stream().map(node -> new DijkstraNode(node, Double.POSITIVE_INFINITY)).collect(Collectors.toList());
        updatedPqueueNodes = new ArrayList<>();
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

        // initialise queue
        pqueue.clear();
        pqueueNodes.get(source.id).distanceFromSource = 0;
        updatedPqueueNodes.add(source.id);
        pqueue.add(pqueueNodes.get(source.id));

        Map<Node, Double> results = new HashMap<>();

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();
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
                    dNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueueNodes.set(neighbour.id, dNeighbour);
                    pqueue.add(dNeighbour);
                    updatedPqueueNodes.add(neighbour.id);
                }
            }
        }
        for (int index : updatedPqueueNodes) {
            DijkstraNode dNode = pqueueNodes.get(index);
            dNode.distanceFromSource = Double.POSITIVE_INFINITY;
            dNode.wasProcessed = false;
        }
        updatedPqueueNodes.clear();
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
        // initialise queue
        pqueue.clear();
        for (Node source : sources) {
            pqueueNodes.get(source.id).distanceFromSource = 0;
            updatedPqueueNodes.add(source.id);
            pqueue.add(pqueueNodes.get(source.id));
        }

        // map to backtrack shortest path
        Map<Integer, Integer> parentMap = new HashMap<>();

        // the first reached node in targets
        Integer backtrackId = null;

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
            if (targets.contains(currentMin.node)) { // TODO find better test than targets.contains
                backtrackId = currentMin.node.id;
                break;
            }
            // get and potentially update all neighbours
            for (Node neighbour : graph.adjList.get(currentMin.node.id)) {
                double alternativeDistance = currentMin.distanceFromSource + currentMin.node.getDistance(neighbour);
                // update node, if the new path is shorter than the previous shortest
                DijkstraNode dNeighbour = pqueueNodes.get(neighbour.id);
                if (alternativeDistance < dNeighbour.distanceFromSource) {
                    dNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueueNodes.set(neighbour.id, dNeighbour);
                    pqueue.add(dNeighbour);
                    updatedPqueueNodes.add(neighbour.id);
                    parentMap.put(neighbour.id, currentMin.node.id); // TODO returns previous value for that key
                }
            }
        }

        if (backtrackId == null) {
            // no route found
            return graph.new Path();
        }

        // reconstruct the path from the target
        LinkedList<DijkstraNode> results = new LinkedList<>();
        do {
            DijkstraNode backtrack = pqueueNodes.get(backtrackId);
            results.addFirst(new DijkstraNode(backtrack.node, backtrack.distanceFromSource));
            backtrackId = parentMap.get(backtrackId);
        } while (backtrackId != null);

        for (int index : updatedPqueueNodes) {
            DijkstraNode dNode = pqueueNodes.get(index);
            dNode.distanceFromSource = Double.POSITIVE_INFINITY;
            dNode.wasProcessed = false;
        }
        updatedPqueueNodes.clear();
        return graph.new Path(results);
    }
}
