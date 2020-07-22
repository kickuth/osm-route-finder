package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Graph.Path;
import eu.kickuth.mthesis.graph.Node;

import java.util.HashSet;
import java.util.Set;

public class GASolver extends Solver {

    public GASolver(Node source, Node target, double maxDistance, Graph g) {
        super(source, target, maxDistance, g);
    }

    public GASolver(Graph g) {
        super(g);
    }

    @Override
    public Path solve() {
        logger.debug("Solving");

        double[] forwardsCosts = dijkstra.getUpdateForwardCosts();
        Node latestPoi = this.getTarget();

        Path solutionPath = graph.new Path();
        Set<Node> poiCandidates = new HashSet<>(reachablePois);

        // TODO propper logging, remove souts
        // TODO do continuous dijkstra growing instead and remove retries
        int retries = 25;

        StringBuilder stuck = new StringBuilder();
        while (!poiCandidates.isEmpty()) {
            Path fromNewPoi = dijkstra.shortestPath(poiCandidates, latestPoi, true);
            Node newPoi = fromNewPoi.getFirst();
            if (fromNewPoi.getPathCost() + solutionPath.getPathCost() + forwardsCosts[newPoi.id] <= maxDistance) {
                latestPoi = newPoi;
                solutionPath = fromNewPoi.append(solutionPath);
                // remove possible targets with the same class as the new node and too distant targets
                double currentSolCost = solutionPath.getPathCost();
                poiCandidates.removeIf(node -> node.type.equals(newPoi.type) || forwardsCosts[node.id] + currentSolCost > maxDistance);

                setStatus(solutionPath.getPathCost()/maxDistance);
                logger.trace(String.format("solving: %.2f%%, %d possible POIs left.", solutionPath.getPathCost()*100/maxDistance, poiCandidates.size()));
            } else {
                poiCandidates.remove(newPoi);
                stuck.append(poiCandidates.size()).append(" ");
                if (--retries < 1) {
                    logger.trace("Stuck and out of retries: {}giving up.", stuck.toString());
                    break;
                }
            }
        }

        solutionPath = dijkstra.shortestPath(this.getSource(), latestPoi, true).append(solutionPath);
        setStatus(0);
        return solutionPath;
    }

    @Override
    public String toString() {
        return "GA Solver";
    }
}
