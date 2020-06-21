package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SPESolver extends Solver {


    public SPESolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);
    }

    public SPESolver(Graph g) {
        super(g);
    }

    public Path solve() {
        logger.debug("Solving");
        Path solutionPath = dijkstra.getShortestPath();
        if (solutionPath.isEmpty() || solutionPath.getPathCost() > maxDistance) {
            logger.info("Target is not reachable!");
            return graph.new Path();
        }

        // get a shallow copy of all POIs
        Set<Node> targets = new HashSet<>(reachablePois);

        // get POIs on shortest path
        Set<Node> initialVisitedPois = graph.getPoisOnPath(solutionPath);

        // remove nodes with classes we have already visited
        for (Node visitedPoi : initialVisitedPois) {
            targets.removeIf(possibleTarget -> possibleTarget.type.equals(visitedPoi.type));
        }

        // keep adding shortest paths to new classes until we would run over the maximal distance
        while (solutionPath.getPathCost() < maxDistance && !targets.isEmpty()) {
            // find a new node starting anywhere on our current path
            Path pathToNewPoi = dijkstra.shortestPath(solutionPath.getNodes(), targets, false);
            // stop if we can't find new POIs
            if (pathToNewPoi.isEmpty()) {
                logger.trace("No new POI classes are reachable!");
                break;
            }
            Node newPoi = pathToNewPoi.getLast();
            // find shortest way back to where our detour started
            Path backPath = dijkstra.shortestPath(newPoi, pathToNewPoi.getFirst(), false);
            // remove target POI, if no path back exists
            if (backPath.isEmpty()) {
                targets.remove(newPoi);
                continue;
            }

            // remove possible targets with the same class as the new node
            targets.removeIf(node -> node.type.equals(newPoi.type));

            // insert the detour into the previous path
            pathToNewPoi.append(backPath);

            // check if inserting the current detour would be too long
            if (pathToNewPoi.getPathCost() + solutionPath.getPathCost() > maxDistance) {
                break;
            }

            // find index for insertion
            int insertPoint = solutionPath.getNodes().indexOf(pathToNewPoi.getFirst());

            solutionPath.insert(pathToNewPoi, insertPoint, insertPoint);

            // print estimated progress
            setStatus(solutionPath.getPathCost()/maxDistance);
            logger.trace(String.format("solving: %.2f%%", getStatus()*100));
        }

        setStatus(0.0);
        return solutionPath;
    }

    @Override
    public String toString() {
        return "SPE Solver";
    }
}
