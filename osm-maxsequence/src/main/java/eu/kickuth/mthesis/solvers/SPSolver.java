package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;

public class SPSolver extends Solver {


    public SPSolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);
    }

    public SPSolver(Graph g) {
        super(g);
    }

    public Path solve() {
        logger.debug("Solving");
        Path solutionPath = dijkstra.getShortestPath();
        setStatus(0.0);
        if (solutionPath.isEmpty() || solutionPath.getPathCost() > maxDistance) {
            logger.info("Target is not reachable!");
            return graph.new Path();
        } else {
            return solutionPath;
        }
    }

    @Override
    public String toString() {
        return "Shortest Path Solver";
    }
}
