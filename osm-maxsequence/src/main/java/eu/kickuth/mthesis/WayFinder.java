package eu.kickuth.mthesis;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

public class WayFinder {

    private static final Logger logger = LogManager.getLogger(WayFinder.class.getName());

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

    public WayFinder(Graph g, Node source, Node target, double maxDistance) {
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

        // Find out which classes we have visited and which nodes have a class
        Set<String> currentUniquePois = new HashSet<>();
        Set<Node> sources = new HashSet<>();
        for (DijkstraNode site : shortestPath) {
            String type = site.node.getType();
            if (!StringUtils.isEmpty(type)) {
                currentUniquePois.add(type);
                sources.add(site.node);
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
        double currentDistance = shortestPath.get(shortestPath.size() - 1).distanceFromSource;
        while (currentDistance < maxDistance && !targets.isEmpty()) {
            List<DijkstraNode> pathToNewPoi = dijkstra.shortestPath(sources, targets);
            Node newPoi = pathToNewPoi.get(pathToNewPoi.size()-1).node;
            //find shortest way back
            List<DijkstraNode> backPath = dijkstra.shortestPath(newPoi, pathToNewPoi.get(0).node);
            // remove possible targets with the same class as the new node
            targets.removeIf(node -> node.getType().equals(newPoi.getType()));
            // insert the detour into the previous path
            appendPath(pathToNewPoi, backPath);
            // find index for insertion
            Iterator<DijkstraNode> iter = shortestPath.iterator();
            int insertIndex = 0;
            DijkstraNode lookingFor = pathToNewPoi.get(0);
            while (iter.hasNext()) {
                DijkstraNode dNode = iter.next();
                if (dNode.node.equals(lookingFor.node)) {
                    break;
                }
                insertIndex++;
            }

            insertPath(shortestPath, pathToNewPoi, insertIndex);
            // print estimated progress
            currentDistance = shortestPath.get(shortestPath.size() - 1).distanceFromSource;
            logger.debug(String.format("Naive greedy: %.2f%%", currentDistance*100/maxDistance));
        }
        return shortestPath.stream().map(dNode -> dNode.node).collect(Collectors.toList());
    }

    private static void appendPath(List<DijkstraNode> toUpdate, List<DijkstraNode> toAppend) {
        DijkstraNode lastNode = toUpdate.get(toUpdate.size()-1);
        // if end and starting node are equal, only keep one
        if (lastNode.node.equals(toAppend.get(0).node)) {
            toAppend.remove(0);
        }
        toAppend.forEach(dNode -> dNode.distanceFromSource += lastNode.distanceFromSource);
        toUpdate.addAll(toAppend);
    }

    /**
     * Merge two paths. More specifically merges the second path into the first path, adjusting node costs.
     * Requires first and last node of the inserted list to be present in both lists.
     * @param toUpdate the list that we merge into
     * @param toInsert the list we want to add. Must be a circle, i.e. start and end node are equal.
     * @param insertAtIndex the position after which the second list is inserted
     */
    private static void insertPath(List<DijkstraNode> toUpdate, List<DijkstraNode> toInsert, int insertAtIndex) {
        if (!toInsert.get(toInsert.size()-1).node.equals(toInsert.get(0).node)) {
            throw new IllegalArgumentException("Inserted path is not a circle.");
        }

        // remove duplicate node
        toInsert.remove(0);

        // update the costs of nodes following the inserted part
        double costOfInsertion = toInsert.get(toInsert.size()-1).distanceFromSource;
        for (int i = insertAtIndex+1; i < toUpdate.size(); i++) {
            toUpdate.get(i).distanceFromSource += costOfInsertion;
        }

        // update the costs for the path that we will insert
        double costAtInsertion = toUpdate.get(insertAtIndex).distanceFromSource;
        toInsert.forEach((dNode)->{dNode.distanceFromSource += costAtInsertion;});

        // insert nodes
        toUpdate.addAll(insertAtIndex+1, toInsert);
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
