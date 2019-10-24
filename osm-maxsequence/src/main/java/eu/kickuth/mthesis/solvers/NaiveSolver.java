package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import eu.kickuth.mthesis.graph.Graph.Path;

public class NaiveSolver extends Solver {


    private final Dijkstra dijkstra;

    public NaiveSolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);

        dijkstra = new Dijkstra(g);
    }

    // TODO improve/rewrite, comment
    public Path solve() {
        logger.debug("Solving");
        Path shortestPath = dijkstra.shortestPath(source, target);
        if (shortestPath.isEmpty() || shortestPath.getPathCost() > maxDistance) {
            System.out.println("Target is not reachable!");
            return searchGraph.new Path();
        }

        // get a copy of all POIs
        Set<Node> targets = new HashSet<>(searchGraph.getPois());

        // get POIs on shortest path
        Set<Node> initialVisitedPois = searchGraph.getPoisOnPath(shortestPath);

        // allow path insertions at visited POIs, start and end node
        Set<Node> sources = new HashSet<>(initialVisitedPois);
        sources.add(shortestPath.getFirst());
        sources.add(shortestPath.getLast());

        // remove nodes with classes we have already visited
        for (Node visitedPoi : initialVisitedPois) {
            targets.removeIf(possibleTarget -> possibleTarget.getType().equals(visitedPoi.getType()));
        }

        // keep adding shortest paths to new classes until we run over the maximal distance
        // TODO will currently overshoot maximal distance
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
            targets.removeIf(node -> node.getType().equals(newPoi.getType()));
            // insert the detour into the previous path
            pathToNewPoi.append(backPath);
            // find index for insertion
            int insertStart = shortestPath.getNodes().indexOf(pathToNewPoi.getFirst());
            int insertEnd = shortestPath.getNodes().indexOf(pathToNewPoi.getLast());

            if (insertStart < 0 || insertEnd < 0) {
                System.err.println("BUG!");
                // TODO investigate bug (values -1 for both, bw map, 1500 km, default start/end nodes)
            }

            shortestPath.insert(pathToNewPoi, insertStart, insertEnd);

            // print estimated progress
            status = shortestPath.getPathCost()/maxDistance;
            logger.trace(String.format("solving: %.2f%%", status*100));
        }

        status = 0;
        return shortestPath;
    }

    @Override
    public String getName() {
        return "Naive Solver";
    }
}
