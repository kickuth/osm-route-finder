package eu.kickuth.mthesis;

public class Node {

    private final long id;
    private final double lat;
    private final double lon;
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


        double lat2 = n.getLat();
        double lon2 = n.getLon();
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
        return (int) (id % Integer.MAX_VALUE);
    }
}
