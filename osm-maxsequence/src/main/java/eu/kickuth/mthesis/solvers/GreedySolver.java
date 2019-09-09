package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GreedySolver extends Solver {

    private Dijkstra dijkstra;
    private Map<Node, Double> estimatedDistanceToTarget;

    public GreedySolver(Node source, Node target, double maxDistance, Graph g) {
        setup(source, target, maxDistance, g);
    }

    @Override
    public void setup(Node source, Node target, double maxDistance, Graph g) {
        super.setup(source, target, maxDistance, g);
        // TODO limit graph? // fix graph.clone?!
        dijkstra = new Dijkstra(searchGraph);
        estimatedDistanceToTarget = dijkstra.sssp(target, maxDistance);



    }

    @Override
    public List<Node> solve() {
        // find all nodes with classes
        Set<Node> poiNodes = new HashSet<>();
        for (Node node : searchGraph.adjList.keySet()) {
            String type = node.getType();
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
                sol.append(dijkstra.shortestPath(currentEnd, target));
                break;
            }
            Node newPoi = pathToNewPoi.getLast();
            if (sol.getPathCost() + pathToNewPoi.getPathCost() + estimatedDistanceToTarget.get(newPoi) < maxDistance) {
                // remove possible targets with the same class as the new node
                poiNodes.removeIf(node -> node.getType().equals(newPoi.getType()));
                // append new path
                sol.append(pathToNewPoi);
                currentEnd = sol.getLast();
                logger.trace(String.format("solving: %.2f%%", sol.getPathCost()*100/maxDistance));
            } else {
                poiNodes.remove(newPoi);
                if (retries-- <= 0) {
                    break;
                }
            }
        }

        logger.info("Unique class score for greedy algorithm: {}", uniqueClassScore(sol));
        return sol.getNodes();
    }
}
