package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;

import java.util.HashSet;
import java.util.Set;

public class NaiveSolver extends Solver {


    public NaiveSolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);
    }

    public Path solve() {
        logger.debug("Solving");
        Path shortestPath = dijkstra.shortestPath(source, target);
        if (shortestPath.isEmpty() || shortestPath.getPathCost() > maxDistance) {
            logger.info("Target is not reachable!");
            return graph.new Path();
        }

        // get a shallow copy of all POIs
        Set<Node> targets = new HashSet<>(graph.pois);

        // get POIs on shortest path
        Set<Node> initialVisitedPois = graph.getPoisOnPath(shortestPath);

        // allow path insertions at visited POIs, start and end node
        Set<Node> sources = new HashSet<>(initialVisitedPois);
        sources.add(shortestPath.getFirst());
        sources.add(shortestPath.getLast());

        // remove nodes with classes we have already visited
        for (Node visitedPoi : initialVisitedPois) {
            targets.removeIf(possibleTarget -> possibleTarget.type.equals(visitedPoi.type));
        }

        // keep adding shortest paths to new classes until we would run over the maximal distance
        while (shortestPath.getPathCost() < maxDistance && !targets.isEmpty()) {
            Path pathToNewPoi = dijkstra.shortestPath(sources, targets);
            // stop if we can't find new POIs
            if (pathToNewPoi.isEmpty()) {
                logger.trace("No new POI classes are reachable!");
                break;
            }
            Node newPoi = pathToNewPoi.getLast();
            // find shortest way back
            Path backPath = dijkstra.shortestPath(newPoi, sources);
            // remove target POI, if no path back exists
            if (backPath.isEmpty()) {
                targets.remove(newPoi);
                continue;
            }
            sources.add(newPoi);

            // remove possible targets with the same class as the new node
            targets.removeIf(node -> node.type.equals(newPoi.type));

            // insert the detour into the previous path
            pathToNewPoi.append(backPath);

            // check if the path might grow too large
            if (pathToNewPoi.getPathCost() + shortestPath.getPathCost() > maxDistance) {
                break;
            }

            // find index for insertion
            int insertStart = shortestPath.getNodes().lastIndexOf(pathToNewPoi.getFirst());
            int insertEnd = shortestPath.getNodes().indexOf(pathToNewPoi.getLast());

            shortestPath.insert(pathToNewPoi, insertStart, insertEnd);

            // print estimated progress
            setStatus(shortestPath.getPathCost()/maxDistance);
            logger.trace(String.format("solving: %.2f%%", getStatus()*100));
        }

        setStatus(0.0);
        return shortestPath;
    }

    @Override
    public String toString() {
        return "Naive Solver";
    }
}
