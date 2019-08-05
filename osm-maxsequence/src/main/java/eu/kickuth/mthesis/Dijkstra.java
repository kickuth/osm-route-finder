package eu.kickuth.mthesis;

import java.util.PriorityQueue;

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
        double[] dist = new double[graph.adjList.size()];
        // add all nodes to the queue and set dist[source] to 0
        for (Node node : graph.adjList.keySet()) {
            dist[node.getId()] = (node.equals(source) ? 0 : Integer.MAX_VALUE);
            pqueue.add(new DijkstraNode(node, dist[node.getId()]));
        }

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode current_min = pqueue.poll();
            current_min.visited = true;
            for (Node neighbour : graph.adjList.get(current_min.node)) {
                double alternativeDistance = current_min.tentativeDistanceFromSource + current_min.node.getDistance(neighbour);
                if (alternativeDistance < dist[neighbour.getId()]) {
                    pqueue.add(new DijkstraNode(neighbour, alternativeDistance));
                    dist[neighbour.getId()] = alternativeDistance;
                }
            }
        }
        int counter = 0;
        for (int i = 0; i < dist.length; i++) {
            if (dist[i] < Integer.MAX_VALUE) {
                counter++;
            }
        }
        System.out.println(counter);
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
