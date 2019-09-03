package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

import eu.kickuth.mthesis.graph.Graph.Path;

public class NaiveSolver implements Solver {

    private static final Logger logger = LogManager.getLogger(NaiveSolver.class.getName());

    public Graph getSearchGraph() {
        return searchGraph;
    }

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

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    private Graph searchGraph;
    private Node source;
    private Node target;
    private double maxDistance;

    private final Dijkstra dijkstra;

    public NaiveSolver(Graph g, Node source, Node target, double maxDistance) {
        searchGraph = g;
        this.source = source;
        this.target = target;
        this.maxDistance = maxDistance;

        dijkstra = new Dijkstra(g);
        limitMap(maxDistance);
    }

    private void limitMap(double maxDistance) {
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

        searchGraph = searchGraph.createSubgraph(reachableSet);
    }

    public List<Node> shortestPath() {
        return dijkstra.shortestPath(source, target).getNodes();
    }

    // TODO improve/rewrite, comment
    public List<Node> solve() {
        Path shortestPath = dijkstra.shortestPath(source, target);
        if (shortestPath.isEmpty() || shortestPath.getPathCost() > maxDistance) {
            System.out.println("Target is not reachable!");
            return new LinkedList<>();
        }

        // Find out which classes we have visited and which nodes have a class
        Set<String> currentUniquePois = new HashSet<>();
        Set<Node> sources = new HashSet<>();
        for (Node site : shortestPath.getNodes()) {
            String type = site.getType();
            if (!StringUtils.isEmpty(type)) {
                currentUniquePois.add(type);
                sources.add(site);
            }
        }

        // find all nodes with classes we haven't visited yet
        Set<Node> targets = new HashSet<>();
        for (Node node : searchGraph.adjList.keySet()) {
            String type = node.getType();
            if (!StringUtils.isEmpty(type) && !currentUniquePois.contains(type)) {
                targets.add(node);
            }
        }

        // keep adding shortest paths to new classes until we run over the maximal distance
        // TODO will currently overshoot maximal distance
        while (shortestPath.getPathCost() < maxDistance && !targets.isEmpty()) {
            Path pathToNewPoi = dijkstra.shortestPath(sources, targets);
            Node newPoi = pathToNewPoi.getNodes().getLast();
            //find shortest way back
            Path backPath = dijkstra.shortestPath(newPoi, sources);
            // remove possible targets with the same class as the new node
            targets.removeIf(node -> node.getType().equals(newPoi.getType()));
            // insert the detour into the previous path
            pathToNewPoi.append(backPath);
            // find index for insertion
            int insertStart = shortestPath.getNodes().indexOf(pathToNewPoi.getNodes().getFirst());
            int insertEnd = shortestPath.getNodes().indexOf(pathToNewPoi.getNodes().getLast());

            // TODO fix this in Path.insert
            shortestPath = shortestPath.insert(pathToNewPoi, insertStart, insertEnd);

            // print estimated progress
            logger.trace(String.format("Naive greedy: %.2f%%", shortestPath.getPathCost()*100/maxDistance));
        }
        return shortestPath.getNodes();
    }

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(List<Node> path) {
        // check if the path nodes are valid
        if (!searchGraph.adjList.keySet().containsAll(path) || path.isEmpty()) {
            System.err.println("Path contains invalid nodes!");
            return -1;
        }
        // check if the path edges are valid
        Iterator<Node> iter = path.iterator();
        Node current = iter.next();
        while (iter.hasNext()) {
            Node next = iter.next();
            if (!searchGraph.adjList.get(current).contains(next)) {
                // edge does not exist
                logger.error("Path to score contains non-existing edges!");
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
