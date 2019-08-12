package eu.kickuth.mthesis;

import java.util.*;

public class Dijkstra {

    private final Graph graph;
    private Node source;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final Map<Node, DijkstraNode> lookup;

    public Dijkstra(Graph graph, Node source) {
        this.graph = graph;
        this.source = source;
        pqueue = new PriorityQueue<>(graph.adjList.size());
        lookup = new HashMap<>(graph.adjList.size());
    }


    /**
     * Compute the single source shortest path to all reachable nodes.
     * @return map of reachable nodes to their minimal distance from the source
     */
    public Map<Node, Double> sssp() {
        return sssp(Double.MAX_VALUE);
    }

    /**
     * Compute the single source shortest path to all nodes within the maxDistance.
     * @param maxDistance maximal distance from source; nodes with larger distance are ignored
     * @return map of reachable nodes within maxDistance to their minimal distance from the source.
     */
    public Map<Node, Double> sssp(double maxDistance) {

        initDijkstra();

        Map<Node, Double> results = new HashMap<>();

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();
            // check if only unreachable nodes are left and we are done
            if (currentMin.tentativeDistanceFromSource == Double.MAX_VALUE) {
                break;
            }
            // check whether an updated node has already been processed
            if (lookup.get(currentMin.node) != currentMin) {
                continue;
            }
            results.put(currentMin.node, currentMin.tentativeDistanceFromSource);
            for (Node neighbour : graph.adjList.get(currentMin.node)) {
                double alternativeDistance = currentMin.tentativeDistanceFromSource + currentMin.node.getDistance(neighbour);
                // ignore nodes outside of maxDistance
                if (alternativeDistance > maxDistance) {
                    continue;
                }
                // update node, if the new path is shorter than the previous shortest
                if (alternativeDistance < lookup.get(neighbour).tentativeDistanceFromSource) {
                    DijkstraNode updatedNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueue.add(updatedNeighbour);
                    lookup.put(neighbour, updatedNeighbour);
                }
            }
        }
        return results;
    }

    /**
     * Compute the shortest s-t-path
     * @param target the target node.
     * @return list of nodes from source to target, empty list if no path exists
     */
    public List<Node> shortestPath(Node target) {

        initDijkstra();

        // map to backtrack shortest path
        Map<Node, Node> previousNode = new HashMap<>();
        previousNode.put(source, null);

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
            if (currentMin.node.equals(target)) {
                break;
            }
            // check whether only unreachable nodes are left --> Target unreachable
            if (currentMin.tentativeDistanceFromSource == Double.MAX_VALUE) {
                return new LinkedList<>();
            }
            for (Node neighbour : graph.adjList.get(currentMin.node)) {
                double alternativeDistance = currentMin.tentativeDistanceFromSource + currentMin.node.getDistance(neighbour);
                // update node, if the new path is shorter than the previous shortest
                if (alternativeDistance < lookup.get(neighbour).tentativeDistanceFromSource) {
                    DijkstraNode updatedNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueue.add(updatedNeighbour);
                    lookup.put(neighbour, updatedNeighbour);
                    previousNode.put(neighbour, currentMin.node);
                }
            }
        }

        // reconstruct the path from the target
        // TODO use LinkedHashMap to also store each distance?
        List<Node> results = new LinkedList<>();
        Node backtrack = target;
        while (backtrack != null) {
            results.add(0, backtrack);
            backtrack = previousNode.get(backtrack);
        }
        return results;
    }


    /**
     * Initialise queue with source and initialise lookup table
     */
    private void initDijkstra() {
        for (Node node : graph.adjList.keySet()) {
            DijkstraNode dNode;
            if (node.equals(source)) {
                dNode = new DijkstraNode(node, 0);
                pqueue.add(dNode);
            } else {
                dNode = new DijkstraNode(node, Double.MAX_VALUE);
            }
            lookup.put(node, dNode);
        }
    }


    /**
     * Getter for the Dijkstra source node
     * @return source node
     */
    public Node getSource() {
        return source;
    }

    /**
     * Setter for the Dijkstra source node
     * @param newSource new source node
     */
    public void setSource(Node newSource) {
        source = newSource;
    }


    /**
     * A wrapper class for Node that includes a distance-from-source field and implements Comparable for use in a
     * PriorityQueue
     */
    private class DijkstraNode implements Comparable<DijkstraNode> {
        final Node node;
        double tentativeDistanceFromSource;
        DijkstraNode(Node node, double tentativeDistanceFromSource) {
            this.node = node;
            this.tentativeDistanceFromSource = tentativeDistanceFromSource;
        }

        @Override
        public int compareTo(DijkstraNode other) {
            if (this.tentativeDistanceFromSource < other.tentativeDistanceFromSource) {
                return -1;
            }
            return (this.tentativeDistanceFromSource == other.tentativeDistanceFromSource ? 0 : 1);
        }
    }
}
