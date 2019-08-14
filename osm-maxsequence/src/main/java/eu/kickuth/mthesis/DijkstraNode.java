package eu.kickuth.mthesis;


/**
 * A wrapper class for Node that includes a distance-from-source field and implements Comparable for use in a
 * PriorityQueue
 */
public class DijkstraNode implements Comparable<DijkstraNode> {
    final Node node;

    double distanceFromSource;
    DijkstraNode(Node node, double distanceFromSource) {
        this.node = node;
        this.distanceFromSource = distanceFromSource;
    }

    @Override
    public int compareTo(DijkstraNode other) {
        if (this.distanceFromSource < other.distanceFromSource) {
            return -1;
        }
        return (this.distanceFromSource == other.distanceFromSource ? 0 : 1);
    }
}
