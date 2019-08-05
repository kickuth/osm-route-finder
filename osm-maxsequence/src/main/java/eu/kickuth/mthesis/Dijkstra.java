package eu.kickuth.mthesis;

import java.util.PriorityQueue;
import java.util.Queue;

public class Dijkstra {

    private final Graph graph;
    private final Node source;
    private final PriorityQueue<DijkstraNode> pqueue;

    public Dijkstra(Graph graph, Node source) {
        this.graph = graph;
        this.source = source;
        pqueue = new PriorityQueue<>(graph.adjList.size());
    }


    public void sssp() {
        // add all nodes to the queue and set dist[source] to 0
        for (Node node : graph.adjList.keySet()) {
            pqueue.add(new DijkstraNode(node, (node.equals(source) ? 0 : Integer.MAX_VALUE)));
        }

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode current_min = pqueue.poll();
            for (Node neighbour : graph.adjList.get(current_min.node)) {
                double alternativeDistance = current_min.tentativeDistanceFromSource + current_min.node.getDistance(neighbour);
                if (alternativeDistance < 0) {// TODO replace 0 with current cost of neighbour
                    pqueue.add(new DijkstraNode(neighbour, alternativeDistance));
                }
            }
        }
    }


    public Node getSource() {
        return source;
    }


    private class DijkstraNode implements Comparable<DijkstraNode> {
        final Node node;
        double tentativeDistanceFromSource;
        boolean visited = false;
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
