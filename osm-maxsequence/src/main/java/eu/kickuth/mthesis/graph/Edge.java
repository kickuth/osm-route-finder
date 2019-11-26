package eu.kickuth.mthesis.graph;

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

    @Override
    public boolean equals(Object obj) {
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        } else {
            return source.equals(((Edge) obj).source) && dest.equals(((Edge) obj).dest);
        }
    }

    @Override
    public int hashCode() {
        return  source.hashCode() * 3 + dest.hashCode();
    }

    public String toString() {
        return "Edge: " + source + " to " + dest + " cost: " + cost;
    }

}