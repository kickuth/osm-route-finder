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


    public Map<Node, Double> sssp() {
        return sssp(Double.MAX_VALUE);
    }

    public Map<Node, Double> sssp(double maxDistance) {
        // initialise queue with source and dist[source] = 0 and initialise lookup table
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
                System.out.println("updated node already processed (decrease key alternative)");
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


    public Node getSource() {
        return source;
    }

    public void setSource(Node newSource) {
        source = newSource;
    }


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
