package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;

import java.util.*;

public class SmartSPESolver extends Solver {

    public SmartSPESolver(Node source, Node target, double maxDistanceFactor, Graph graph) {
        super(source, target, maxDistanceFactor, graph);
    }

    public SmartSPESolver(Graph g) {
        super(g);
    }

    @Override
    public Graph.Path solve() {
        logger.debug("Solving");
        Graph.Path solutionPath = dijkstra.getShortestPath();
        if (solutionPath.isEmpty() || solutionPath.getPathCost() > maxDistance) {
            logger.info("Target is not reachable!");
            return graph.new Path();
        }

        // copy of solution that will be returned once the solutionPath overshoots the maximum distance
        Graph.Path currentPath = solutionPath.copy();

        // get a shallow copy of all POIs
        Set<Node> targets = new HashSet<>(reachablePois);

        // get POIs on shortest path
        Set<Node> initialPOIs = graph.getPoisOnPath(solutionPath);

        // keep a map of included POI classes and their count
        Map<String, Integer> pathClassCount = new HashMap<>();
        initialPOIs.forEach(node -> {
            if (node.type != null) {
                // merge: if already present, increase by one; else store with value 1
                pathClassCount.merge(node.type, 1, Integer::sum);
            }
        });

        // remove targets with classes, that we have already visited
        for (String visitedClass : pathClassCount.keySet()) {
            targets.removeIf(possibleTarget -> possibleTarget.type.equals(visitedClass));
        }

        // keep adding shortest paths to new classes until we run out of targets or would go over the maximal distance
        do {
            // get new POI closest to current path
            Graph.Path pathToNewPoi = dijkstra.shortestPath(solutionPath.getNodes(), targets, false);
            // stop if we can't find new POIs
            if (pathToNewPoi.isEmpty()) {
                logger.debug("No new POI classes are reachable!");
                break;
            }
            Node newPOI = pathToNewPoi.getLast();
            LinkedList<Node> solutionNodes = solutionPath.getNodes();
            int insertStart = solutionNodes.indexOf(pathToNewPoi.getFirst());

            ListIterator<Node> fwdIter = solutionNodes.listIterator(insertStart);
            ListIterator<Node> bwdIter = solutionNodes.listIterator(insertStart);

            int insertEnd = insertStart-1;

            Node insertEndNode = getTarget();  // initialize as target, in case our insert start is the target
            while (fwdIter.hasNext()) {
                insertEndNode = fwdIter.next();
                insertEnd++;
                if (insertEndNode.type == null) {
                    continue;
                }
                int currentCount = pathClassCount.get(insertEndNode.type);
                if (currentCount != 1) {
                    // we will drop this POI by inserting a new path around it, so reduce class count
                    pathClassCount.put(insertEndNode.type, currentCount-1);
                } else {
                    // this is where we want our insertion path to end, to avoid losing already collected classes
                    break;
                }
            }

            Node insertStartNode = getSource();  // initialize as source, in case our insert start is the source
            while (bwdIter.hasPrevious()) {
                insertStartNode = bwdIter.previous();
                insertStart--;
                if (insertStartNode.type == null) {
                    continue;
                }
                int currentCount = pathClassCount.get(insertStartNode.type);
                if (currentCount != 1) {
                    // we will drop this POI by inserting a new path around it, so reduce class count
                    pathClassCount.put(insertStartNode.type, currentCount-1);
                } else {
                    // this is where we want our insertion path to end, to avoid losing already collected classes
                    break;
                }
            }
            Graph.Path newPath = dijkstra.shortestPath(insertStartNode, newPOI, false).append(
                    dijkstra.shortestPath(newPOI, insertEndNode, false));

            // TODO edge case: we can't establish the new path. Fix if occurs.
            if (newPath.isEmpty()) {
                logger.error("new path doesn't exist. Code is broken and needs fixing.");
                return solutionPath;
            }

            currentPath = solutionPath.copy();

            // add newly encountered classes to our map or adjust their count, if already present
            LinkedList<Node> newNodes = newPath.getNodes();
            ListIterator<Node> newPathIter = newNodes.listIterator(1);
            for (int i = 1; i < newNodes.size()-1; i++) {
                Node newPathNode = newPathIter.next();
                if (newPathNode.type != null) {
                    int count = pathClassCount.getOrDefault(newPathNode.type, 0);
                    if (count == 0) {
                        // remove possible targets with the same class as the new node
                        targets.removeIf(node -> node.type.equals(newPathNode.type));
                    }
                    pathClassCount.put(newPathNode.type, count+1);
                }
            }

            solutionPath.insert(newPath, insertStart, insertEnd);

            // print estimated progress
            setStatus(solutionPath.getPathCost()/maxDistance);
            logger.trace(String.format("solving: %.2f%%", getStatus()*100));
        } while (solutionPath.getPathCost() <= maxDistance && !targets.isEmpty());



        setStatus(0.0);
        return solutionPath.getPathCost() <= maxDistance ? solutionPath : currentPath;
    }

    @Override
    public String toString() {
        return "SmartSPE Solver";
    }

}
