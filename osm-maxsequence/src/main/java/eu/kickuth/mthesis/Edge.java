package eu.kickuth.mthesis;

public class Edge {

    private Node source;
    private Node dest;
    private double cost;

    /**
     * Constructs an edge. Uses euclidean distance as cost
     * @param source source node
     * @param dest destination node
     */
    public Edge(Node source, Node dest) {
        this(source, dest, Math.sqrt(
                Math.pow(source.getLat() - dest.getLat(), 2) + Math.pow(source.getLon() - dest.getLon(), 2)));
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

    public Node getSource() {
        return source;
    }

    public Node getDest() {
        return dest;
    }

    public double getCost() {
        return cost;
    }
}
