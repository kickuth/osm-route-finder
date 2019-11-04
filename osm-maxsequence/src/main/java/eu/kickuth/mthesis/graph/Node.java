package eu.kickuth.mthesis.graph;

public class Node {

    public final long id;
    public final double lat;
    public final double lon;
    public final String type;
    private String roadType;

    public Node(long id, double lat, double lon, String type) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.type = (type == null ? null : type.intern());
    }

    /**
     * getter method used by velocity. You may access the id variable directly.
     * @return the node id
     */
    public long getId() {
        return id;
    }

    public String getRoadType() {
        return roadType;
    }

    public void setRoadType(String rt) {
        roadType = rt.intern();
    }

    /**
     * Computes the correct distance between two nodes.
     *
     * Adapted version from
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
     *
     * @param n the node to compare
     * @return the Distance in meters between the nodes.
     */
    public double getDistance(Node n) {
        final int R = 6371;  // earths radius

        double lat2 = n.lat;
        double lon2 = n.lon;
        double latDistance = Math.toRadians(lat2 - lat);
        double lonDistance = Math.toRadians(lon2 - lon);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000;  // converted to meters
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
        return (int) ((id + Integer.MIN_VALUE) % Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        return String.format("Node %d: %s", id, type);
    }
}
