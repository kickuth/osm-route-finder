package eu.kickuth.mthesis;

import java.util.*;

public class Dijkstra {

    private final Graph graph;
    private final Node source;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final Map<Node, DijkstraNode> link;
    private final List<DijkstraNode> results;

    public Dijkstra(Graph graph, Node source) {
        this.graph = graph;
        this.source = source;
        pqueue = new PriorityQueue<>(graph.adjList.size());
        link = new HashMap<>(graph.adjList.size());
        results = new ArrayList<>();  // TODO size?
    }


    public void sssp() {
        // add all nodes to the queue and set dist[source] to 0
        for (Node node : graph.adjList.keySet()) {
            DijkstraNode dNode = new DijkstraNode(node, (node.equals(source) ? 0 : Integer.MAX_VALUE));
            pqueue.add(dNode);
            link.put(node, dNode);
        }

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode current_min = pqueue.poll();
            // check whether an updated node has already been processed
            if (link.get(current_min.node) != current_min) {
                continue;
            }
            results.add(current_min);
            // current_min.visited = true; TODO
            for (Node neighbour : graph.adjList.get(current_min.node)) {
                double alternativeDistance = current_min.tentativeDistanceFromSource + current_min.node.getDistance(neighbour);
                if (alternativeDistance < link.get(neighbour).tentativeDistanceFromSource) {
                    DijkstraNode updatedNeighbour = new DijkstraNode(neighbour, alternativeDistance);
                    pqueue.add(updatedNeighbour);
                    link.put(neighbour, updatedNeighbour);
                }
            }
        }
        System.out.println(results.size());
        // TODO howto: return results;

    }


    public Node getSource() {
        return source;
    }


    private class DijkstraNode implements Comparable<DijkstraNode> {
        final Node node;
        double tentativeDistanceFromSource;
        boolean visited = false; // TODO
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
