package eu.kickuth.mthesis.solvers;

import eu.kickuth.mthesis.graph.Dijkstra;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class Solver {

    Logger logger = LogManager.getLogger(this.getClass().getName());

    Graph searchGraph;
    Node source;
    Node target;
    double maxDistance;

    volatile double status;

    public void setup(long sourceID, long targetID, double maxDistance, Graph g) {
        setup(g.getNode(sourceID), g.getNode(targetID), maxDistance, g);
    }
    public void setup(Node source, Node target, double maxDistance, Graph g) {
        searchGraph = g.clone();
        this.source = source;
        this.target = target;
        this.maxDistance = maxDistance;
    }

    public List<Node> solve() {
        if (maxDistance < 0) {
            throw new IllegalArgumentException();
        }
        return new LinkedList<>();
    }

    /**
     * Simple scoring for a path, that computes the number of unique classes visited
     * @param path The path to score
     * @return number of unique classes on path
     */
    public int uniqueClassScore(Graph.Path path) {
        Set<String> uniqueClasses = new HashSet<>();
        for (Node site : path.getNodes()) {
            String type = site.getType();
            if (!StringUtils.isEmpty(type)) {
                uniqueClasses.add(type);
            }
        }
        return uniqueClasses.size();
    }

    /*
    getters and setters
    */
    public void setSource(long id) {
        source = searchGraph.getNode(id);
    }
    public void setSource(Node source) {
        this.source = source;
    }

    public Node getSource() {
        return source;
    }

    public void setTarget(long id) {
        target = searchGraph.getNode(id);
    }
    public void setTarget(Node target) {
        this.target = target;
    }

    public Node getTarget() {
        return target;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }
    public String getMaxDistanceKM() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(maxDistance / 1000);
    }

    /**
     * Get the progress of the solver.
     * @return double [0, 1] representing the current solving progress
     */
    public double getStatus() {
        return Math.min(status, 1);
    }
}
