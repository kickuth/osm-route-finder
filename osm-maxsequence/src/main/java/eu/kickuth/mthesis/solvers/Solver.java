package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;

public abstract class Solver {

    final Logger logger = LogManager.getLogger(this.getClass().getName());

    final Graph graph;
    final Dijkstra dijkstra;
    Node source;
    Node target;
    double maxDistance;
    private double maxDistanceFactor;

    // variable indicating the solvers progress. Should be within the interval [0, 1] and set to 0 when done solving.
    private volatile double status;

    /**
     * Instantiate a solver.
     * @param source initial starting node
     * @param target initial target node
     * @param maxDistanceFactor initial distance as a factor of the shortest path
     * @param graph the graph to search on
     */
    Solver(Node source, Node target, double maxDistanceFactor, Graph graph) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        dijkstra = Dijkstra.getInstance(this.graph);
        setMaxDistanceFactor(maxDistanceFactor);
    }

    /**
     * Compute path method.
     * @return The solution path
     */
    public abstract Graph.Path solve();

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(Graph.Path path) {
        int roadTypesCount = Math.toIntExact(path.getNodes().stream().map(Node::getRoadType).distinct().count());
        int poiTypesCount = Math.toIntExact(graph.getPoisOnPath(path).stream().map(node -> node.type).distinct().count());
        return roadTypesCount + poiTypesCount;
    }

    /*
    getters and setters
    */
    public void setSource(int id) {
        source = graph.getNode(id);
    }
    public void setSource(Node source) {
        this.source = source;
    }

    public Node getSource() {
        return source;
    }

    public void setTarget(int id) {
        target = graph.getNode(id);
    }
    public void setTarget(Node target) {
        this.target = target;
    }

    public Node getTarget() {
        return target;
    }

    /**
     * Set the maximum distance
     * @param maxDistance maximum distance in meters.
     */
    public void setAbsoluteMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    /**
     * Set the maximum distance as a factor of the shortest path
     * @param shortestPathFactor Length as a factor of the shortest path (i.e. should be > 1)
     */
    public void setMaxDistanceFactor(double shortestPathFactor) {
        if (shortestPathFactor < 1) {
            logger.error("Solver distance is too short! Setting to shortest path length");
            shortestPathFactor = 1;
        } else if (shortestPathFactor > 10) {
            logger.warn("Solver distance is being set very high!");
        }
        maxDistanceFactor = shortestPathFactor;
        double shortestPathDist = dijkstra.shortestPath(source, target).getPathCost();
        maxDistance = shortestPathDist * shortestPathFactor;
        logger.trace("New maxDistance is {}", maxDistance);
    }

    public double getMaxDistanceFactor() {
        return maxDistanceFactor;
    }

    /**
     * Get formatted maximum distance in kilo meters.
     * @return maximum solution path distance
     */
    public String getMaxDistanceKM() {
        DecimalFormat df = new DecimalFormat("#.###");
        return df.format(maxDistance / 1000);
    }

    /**
     * Get maximum distance in meters.
     * @return maximum solution path distance
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Get the progress of the solver.
     * @return double [0, 1] representing the current solving progress
     */
    public double getStatus() {
        return status;
    }

    /**
     * Update the status.
     * @param newStatus new status. Should be within the interval [0,1].
     */
    synchronized void setStatus(double newStatus) {
        status = Math.max(0, Math.min(newStatus, 1));
    }

    public String toString() {
        return "Generic Solver";
    }
}
