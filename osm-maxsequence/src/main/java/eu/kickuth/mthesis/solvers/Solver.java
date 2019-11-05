package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.List;

public abstract class Solver {

    Logger logger = LogManager.getLogger(this.getClass().getName());

    Graph searchGraph;
    Dijkstra dijkstra;
    Node source;
    Node target;
    double maxDistance;
    private double maxDistanceFactor;

    volatile double status;


    public Solver(int sourceID, int targetID, double maxDistanceFactor, Graph g) {
        this(g.getNode(sourceID), g.getNode(targetID), maxDistanceFactor, g);
    }

    public Solver(Node source, Node target, double maxDistanceFactor, Graph g) {
        searchGraph = g;
        this.source = source;
        this.target = target;
        dijkstra = new Dijkstra(searchGraph);
        setMaxDistanceFactor(maxDistanceFactor);
    }

    public Graph.Path solve() {
        if (maxDistance < 0) {
            throw new IllegalArgumentException();
        }
        return searchGraph.new Path();
    }

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(Graph.Path path) {
        return searchGraph.getPoisOnPath(path).size();
    }

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(List<Node> path) {
        return searchGraph.getPoisOnPath(path).size();
    }

    /*
    getters and setters
    */
    public void setSource(int id) {
        source = searchGraph.getNode(id);
    }
    public void setSource(Node source) {
        this.source = source;
    }

    public Node getSource() {
        return source;
    }

    public void setTarget(int id) {
        target = searchGraph.getNode(id);
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
        return Math.min(status, 1);
    }

    /**
     * A name representing the solver.
     * @return Name of solver
     */
    public String getName() {
        return "Generic Solver";
    }
}
