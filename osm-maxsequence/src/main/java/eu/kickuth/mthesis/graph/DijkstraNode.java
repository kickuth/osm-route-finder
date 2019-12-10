package eu.kickuth.mthesis.graph;


/**
 * A wrapper class for Node that includes a distance-from-source field and implements Comparable for use in a
 * PriorityQueue
 */
public class DijkstraNode implements Comparable<DijkstraNode> {

    public final Node node;
    public double distanceFromSource;

    DijkstraNode(Node node, double distanceFromSource) {
        this.node = node;
        this.distanceFromSource = distanceFromSource;
    }

    @Override
    public int compareTo(DijkstraNode other) {
        return Double.compare(this.distanceFromSource, other.distanceFromSource);
    }

    @Override
    public boolean equals(Object obj) {
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        } else {
            return this.node.equals(((DijkstraNode) obj).node) &&
                    this.distanceFromSource == ((DijkstraNode) obj).distanceFromSource;
        }
    }

    @Override
    public int hashCode() {
        return (node.hashCode() + (int) distanceFromSource) % Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return node.toString() + " " + distanceFromSource;
    }
}
