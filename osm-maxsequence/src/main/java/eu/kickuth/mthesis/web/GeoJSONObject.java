package eu.kickuth.mthesis.web;

import eu.kickuth.mthesis.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

public class GeoJSONObject {

    private JSONArray features;

    public GeoJSONObject() {
        features = new JSONArray();
    }

    /**
     * Add POIs to the feature object
     * @param pois list of points to add
     */
    public void addPois(Collection<Node> pois) {
        try {
            for (Node node : pois) {
                JSONObject geometry = new JSONObject();
                geometry.put("type", "Point");
                // !! GeoJSON format is longitude then latitude !!
                JSONArray coord = new JSONArray("["+node.getLon()+","+node.getLat()+"]");
                geometry.put("coordinates", coord);

                JSONObject properties = new JSONObject();
                properties.put("name", node.getType());

                JSONObject poi = new JSONObject();
                poi.put("type", "Feature");
                poi.put("geometry", geometry);
                poi.put("properties", properties);

                features.put(poi);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a path to the JSON object.
     * @param path List of coordinates in lat,lon format
     */
    public void addPath(List<double[]> path) {
        try {
            JSONArray coordinates = new JSONArray();
            for (double[] coord : path) {
                // reverse array, because geoJSON works with lon,lat
                double[] lonLat = new double[] {coord[1], coord[0]};
                // add coordinate
                JSONArray coordinate = new JSONArray(lonLat);
                coordinates.put(coordinate);
            }

            // create path and add it to the feature object
            JSONObject jsonPath = new JSONObject();
            jsonPath.put("type", "LineString");
            jsonPath.put("coordinates", coordinates);
            features.put(jsonPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getJSONString() {
        return features.toString();
    }

}
