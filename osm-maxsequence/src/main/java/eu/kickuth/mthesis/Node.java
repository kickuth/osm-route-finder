package eu.kickuth.mthesis;

public class Node {

    private long id;
    private double lat;
    private double lon;
    private String type;

    public Node(long id, double lat, double lon, String type) {
        //this.id = (int) (id % Integer.MAX_VALUE);
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getType() {
        return type;
    }

    /**
     * Compute the euclidean distance between two nodes.
     *
     * @param n the node to compare
     * @return euclidean distance
     */
    public double getDistance(Node n) {
        return Math.sqrt(Math.pow(getLat() - n.getLat(), 2) + Math.pow(getLon() - n.getLon(), 2));
    }

    @Override
    public boolean equals(Object obj) {
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        } else {
            return this.id == ((Node) obj).id;
        }
    }

    @Override
    public int hashCode() {
        return (int) (id % Integer.MAX_VALUE);
    }
}
