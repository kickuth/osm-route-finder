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

        // get a shallow copy of all POIs
        Set<Node> targets = new HashSet<>(reachablePois);

        // get POIs on shortest path
        List<Node> initialPOIs = graph.getOrderedPoisOnPath(solutionPath);

        // places for path insertions
        Set<Node> sources = new HashSet<>(initialPOIs);

        // keep a map of included POI classes and their count
        Map<String, Integer> pathClassCount = new HashMap<>();
        sources.forEach(node -> {
            Integer count;
            if ((count = pathClassCount.get(node.type)) != null) {
                pathClassCount.put(node.type, count+1);
            } else {
                pathClassCount.put(node.type, 1);
            }
        });

        // allow path insertions starting or ending at start or end node
        sources.add(getSource());
        sources.add(getTarget());

        // remove targets with classes, that we have already visited
        for (String visitedClass : pathClassCount.keySet()) {
            targets.removeIf(possibleTarget -> possibleTarget.type.equals(visitedClass));
        }

        // keep adding shortest paths to new classes until we run out of targets or would go over the maximal distance
        while (solutionPath.getPathCost() < maxDistance && !targets.isEmpty()) {
            Graph.Path pathToNewPoi = dijkstra.shortestPath(sources, targets, false);
            // stop if we can't find new POIs
            if (pathToNewPoi.isEmpty()) {
                logger.trace("No new POI classes are reachable!");
                break;
            }
            Node newPOI = pathToNewPoi.getLast();
            LinkedList<Node> solutionNodes = solutionPath.getNodes();
            int insertStart = solutionNodes.indexOf(pathToNewPoi.getFirst());

            ListIterator<Node> iter = solutionNodes.listIterator(insertStart+1);
            int insertEnd = insertStart;

            Node current = getTarget();  // initialize as target, in case our insert start is the target
            while (iter.hasNext()) {
                current = iter.next();
                insertEnd++;
                if (current.type == null) {
                    continue;
                }
                int currentCount = pathClassCount.get(current.type);
                if (currentCount != 1) {
                    // we will drop this POI by inserting a new path around it, so reduce class count
                    pathClassCount.put(current.type, currentCount-1);
                    sources.remove(current);
                } else {
                    // this is where we want our insertion path to end, to avoid losing already collected classes
                    break;
                }
            }
            Graph.Path backPath = dijkstra.shortestPath(newPOI, current, false);

            // TODO edge case: we can't reconnect the path. Fix if occurs.
            if (backPath.isEmpty()) {
                logger.error("back path does not reconnect to current solution. Code is broken and needs fixing.");
                return solutionPath;
            }


            // insert the detour into the previous path
            pathToNewPoi.append(backPath);  // combine paths away from and back to solution

            // check if the path will grow too large
//            // TODO this check sucks. Fix.
//            if (pathToNewPoi.getPathCost() + solutionPath.getPathCost() > maxDistance) {
//                // TODO not a guarantee to stay below maxDistance (if pathToNewPoi is a back path)
//                // TODO fix losing pois/sources bug.
//                break;
//            }



            // add newly encountered classes to our map or adjust their count, if already present
            LinkedList<Node> newNodes = pathToNewPoi.getNodes();
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

            solutionPath.insert(pathToNewPoi, insertStart, insertEnd);

            // print estimated progress
            setStatus(solutionPath.getPathCost()/maxDistance);
            logger.trace(String.format("solving: %.2f%%", getStatus()*100));
        }

        setStatus(0.0);
        return solutionPath;
    }

    @Override
    public String toString() {
        return "SmartSPE Solver";
    }

}
