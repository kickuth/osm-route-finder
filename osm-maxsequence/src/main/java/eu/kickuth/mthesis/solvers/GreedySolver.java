package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;

@Deprecated
public class GreedySolver extends Solver {
    // TODO

    public GreedySolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);
    }

    @Override
    public Path solve() {
        logger.debug("Solving");

        //logger.trace(String.format("solving: %.2f%%", sol.getPathCost()*100/maxDistance));
        setStatus(0);
        return graph.new Path();
    }

    @Override
    public String toString() {
        return "Greedy Solver";
    }
}
