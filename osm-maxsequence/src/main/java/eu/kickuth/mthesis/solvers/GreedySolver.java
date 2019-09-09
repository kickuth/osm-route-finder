package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;

import java.util.List;

public class GreedySolver extends Solver {

    private Dijkstra dijkstra;

    public GreedySolver(Node source, Node target, double maxDistance, Graph g) {
        setup(source, target, maxDistance, g);
    }

    @Override
    public void setup(Node source, Node target, double maxDistance, Graph g) {
        super.setup(source, target, maxDistance, g);
        // TODO limit graph? // fix graph.clone?!
        dijkstra = new Dijkstra(searchGraph);
    }

    @Override
    public List<Node> solve() {

        return null;  // TODO
    }
}
