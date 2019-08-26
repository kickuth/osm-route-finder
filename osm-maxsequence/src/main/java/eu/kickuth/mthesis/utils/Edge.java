package eu.kickuth.mthesis.utils;

public class Edge {

    public final Node source;
    public final Node dest;
    public final double cost;

    /**
     * Constructs an edge. Uses euclidean distance as cost
     * @param source source node
     * @param dest destination node
     */
    public Edge(Node source, Node dest) {
        this(source, dest, source.getDistance(dest));
    }

    /**
     * Constructs an edge.
     * @param source source node
     * @param dest destination node
     * @param cost edge cost
     */
    public Edge(Node source, Node dest, double cost) {
        this.source = source;
        this.dest = dest;
        this.cost = cost;
    }
}
