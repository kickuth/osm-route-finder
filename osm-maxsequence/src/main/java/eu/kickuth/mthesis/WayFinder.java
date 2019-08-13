package eu.kickuth.mthesis;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class WayFinder {

    private Graph graph;

    public Node getSource() {
        return source;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public Node getTarget() {
        return target;
    }

    public void setTarget(Node target) {
        this.target = target;
    }

    private Node source;
    private Node target;
    private Set<Node> nodes;

    private final Dijkstra dijkstra;

    public WayFinder(Graph g, Node source, Node target) {
        graph = g;
        this.source = source;
        this.target = target;

        dijkstra = new Dijkstra(g);
        nodes = g.adjList.keySet();  // TODO does not change when graph changes
    }

    public Graph limitMap(double maxDistance) {
        Map<Node, Double> reachableSourceSet = dijkstra.sssp(source, maxDistance);
        Map<Node, Double> reachableTargetSet = dijkstra.sssp(target, maxDistance);
        Set<Node> reachableSet = new HashSet<>();
        for (Node node : reachableSourceSet.keySet()) {
            if (!reachableTargetSet.containsKey(node)) {
                continue;
            }
            double totalDist = reachableSourceSet.get(node) + reachableTargetSet.get(node);
            if (totalDist <= maxDistance) {
                reachableSet.add(node);
            }
        }

        graph = graph.createSubgraph(reachableSet);
        return graph;
    }


    public List<Node> shortestPath() {
        return dijkstra.shortestPath(source, target);
    }

    public int shortestPathScoring() {
        List<Node> path = dijkstra.shortestPath(source, target);
        return uniqueClassScore(path);
    }

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(List<Node> path) {
        // check if the path nodes are valid
        if (!nodes.containsAll(path) || path.isEmpty()) {
            return -1;
        }
        // check if the path edges are valid
        Iterator<Node> iter = path.iterator();
        Node current = iter.next();
        while (iter.hasNext()) {
            Node next = iter.next();
            if (!graph.adjList.get(current).contains(next)) {
                // edge does not exist
                return -1;
            }
            current = next;
        }

        // count unique classes (simple scoring)
        Set<String> uniqueClasses = new HashSet<>();
        for (Node site : path) {
            String type = site.getType();
            if (!StringUtils.isEmpty(type)) {
                uniqueClasses.add(type);
            }
        }

        return uniqueClasses.size();
    }

}
