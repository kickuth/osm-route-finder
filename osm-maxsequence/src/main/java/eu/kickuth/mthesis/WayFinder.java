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

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    private Node source;
    private Node target;
    private double maxDistance;

    private final Dijkstra dijkstra;

    public WayFinder(Graph g, Node source, Node target, double maxDistance) {
        graph = g;
        this.source = source;
        this.target = target;
        this.maxDistance = maxDistance;

        dijkstra = new Dijkstra(g);
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


    public List<DijkstraNode> shortestPath() {
        return dijkstra.shortestPath(source, target);
    }

    // TODO comment
    public List<Node> naiveGreedyOptimizer() {
        List<DijkstraNode> shortestPath = dijkstra.shortestPath(source, target);
        if (shortestPath.isEmpty() || shortestPath.get(shortestPath.size() - 1).distanceFromSource > maxDistance) {
            System.out.println("Target is not reachable!");
            return new LinkedList<>();
        }
        // List<> currentUniquePois

        double currentPathLength = shortestPath.get(shortestPath.size() - 1).distanceFromSource;
        while (currentPathLength < maxDistance) {
            // TODO compute st path to and from
            continue;
        }
        return null;
    }

    // TODO code stored for reference only; full of bugs and eye cancer
//    // TODO comment/fix/redo. Note, this might be weird if nodes are visited multiple times
//    private List<DijkstraNode> insertPath(List<DijkstraNode> current, List<DijkstraNode> toInsert) {
//        if (current.isEmpty()) {
//            return toInsert;
//        }
//        if (toInsert.isEmpty()) {
//            return current;
//        }
//        DijkstraNode insertFirst = toInsert.get(0);
//        int insertSize = toInsert.size();
//        DijkstraNode insertLast = toInsert.get(insertSize-1);
//        double insertStartCost = 0;
//        double insertCost = insertLast.distanceFromSource;
//        toInsert.remove(0);
//
//        List<DijkstraNode> result = new LinkedList<>();
//
//        Iterator<DijkstraNode> iter = current.iterator();
//
//        while (iter.hasNext()) {
//            DijkstraNode curr = iter.next();
//            if (!curr.node.equals(insertFirst.node)) {
//                result.add(curr);
//            } else {
//                double ic = curr.distanceFromSource;
//                insertStartCost = ic;
//                toInsert.forEach((dNode)->{dNode.distanceFromSource += ic;});
//                result.addAll(toInsert);
//                break;
//            }
//        }
//        insertCost -= insertStartCost;
//        while (iter.hasNext()) {
//            DijkstraNode curr = iter.next();
//            if (curr.node.equals(insertLast.node)) {
//                while (iter.hasNext()) {
//                    DijkstraNode insertMe = iter.next();
//                    insertMe.distanceFromSource += insertCost;
//                    result.add(insertMe);
//                }
//            }
//        }
//        return result;
//    }
//
//    private List<DijkstraNode> mergePaths(List<DijkstraNode> current, List<DijkstraNode> toAppend) {
//        if (current.isEmpty()) {
//            return toAppend;
//        }
//        if (toAppend.isEmpty()) {
//            return current;
//        }
//        DijkstraNode currentEnd = current.get(current.size()-1);
//        if (!currentEnd.node.equals(toAppend.get(0).node)) {
//            throw new IllegalArgumentException("Paths can not be merged!");
//        }
//        toAppend.remove(0);
//        toAppend.forEach((dNode)->{dNode.distanceFromSource += currentEnd.distanceFromSource;});
//        current.addAll(toAppend);
//
//        return current;
//    }

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(List<Node> path) {
        // check if the path nodes are valid
        if (!graph.adjList.keySet().containsAll(path) || path.isEmpty()) {
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
