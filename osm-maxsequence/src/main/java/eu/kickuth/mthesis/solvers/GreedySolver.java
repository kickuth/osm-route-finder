package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GreedySolver extends Solver {

    private Map<Node, Double> estimatedDistanceToTarget;

    public GreedySolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);
        // TODO this should be computed for every node, not only up to maxDistance (see also quickfix below)
        estimatedDistanceToTarget = dijkstra.sssp(target, maxDistance);
    }

    @Override
    public Path solve() {
        logger.debug("Solving");
        // find all nodes with classes
        Set<Node> poiNodes = new HashSet<>();
        for (Node node : searchGraph.adjList.keySet()) {
            String type = node.type;
            if (!StringUtils.isEmpty(type)) {
                poiNodes.add(node);
            }
        }
        Path sol = searchGraph.new Path();
        Node currentEnd = source;

        // if the next node is too far away, how often will we look for another node, hoping it will lie more on the path
        int retries = 50;  // TODO hard-coded..

        while(!poiNodes.isEmpty()) {
            Path pathToNewPoi = dijkstra.shortestPath(currentEnd, poiNodes);
            // stop if we can't find new POIs
            if (pathToNewPoi.isEmpty()) {
                logger.trace("No new POI classes are reachable!");
                // TODO because we only estimate the way back, there might not be a way back and we get stuck.
                sol.append(dijkstra.shortestPath(currentEnd, target));
                break;
            }
            Node newPoi = pathToNewPoi.getLast();
            if (estimatedDistanceToTarget.containsKey(newPoi) &&  // TODO containsKey as quickfix for maxdist fail (above)
                    maxDistance >
                            pathToNewPoi.getPathCost()
                            + estimatedDistanceToTarget.get(newPoi)
                            + sol.getPathCost()) {
                // remove possible targets with the same class as the new node
                poiNodes.removeIf(node -> node.type.equals(newPoi.type));
                // append new path
                sol.append(pathToNewPoi);
                currentEnd = sol.getLast();
                status = sol.getPathCost()/maxDistance;
                logger.trace(String.format("solving: %.2f%%", status*100));
            } else {
                poiNodes.remove(newPoi);
                if (retries-- <= 0) {
                    sol.append(dijkstra.shortestPath(currentEnd, target));
                    break;
                }
            }
        }

        logger.trace(String.format("solving: %.2f%%", sol.getPathCost()*100/maxDistance));
        status = 0;
        return sol;
    }

    @Override
    public String getName() {
        return "Greedy Solver";
    }
}
