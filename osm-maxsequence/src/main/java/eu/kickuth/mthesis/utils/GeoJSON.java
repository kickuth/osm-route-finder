package eu.kickuth.mthesis.utils;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;

public class GeoJSON {

    /**
     * Create a JSON POI list
     * @param pois list of points to add
     */
    public static String createPOIList(Collection<Node> pois) {
        try {
            JSONArray features = new JSONArray();
            for (Node node : pois) {
                JSONObject geometry = new JSONObject();
                geometry.put("type", "Point");
                // !! GeoJSON format is longitude then latitude !!
                JSONArray coord = new JSONArray("["+node.lon+","+node.lat+"]");
                geometry.put("coordinates", coord);

                JSONObject properties = new JSONObject();
                properties.put("name", node.type);
                properties.put("id", node.id);

                JSONObject poi = new JSONObject();
                poi.put("type", "Feature");
                poi.put("geometry", geometry);
                poi.put("properties", properties);

                features.put(poi);
            }
            return features.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * create a JSON path object.
     * @param path List of coordinates in lat,lon format
     */
    public static String createPath(Graph.Path path, Map<String,String> args) {
        try {
            JSONArray coordinates = new JSONArray();
            for (Node node : path.getNodes()) {
                // ! geoJSON works with lon,lat !
                double[] lonLat = new double[] {node.lon, node.lat};
                // add coordinate
                JSONArray coordinate = new JSONArray(lonLat);
                coordinates.put(coordinate);
            }

            // create path and add it to the feature object
            JSONObject jsonPath = new JSONObject();
            jsonPath.put("type", "LineString");
            jsonPath.put("length", Math.floor(path.getPathCost())/1000);
            jsonPath.put("coordinates", coordinates);
            for (Map.Entry<String, String> arg : args.entrySet()) {
                jsonPath.put(arg.getKey(), arg.getValue());
            }

            return jsonPath.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
}
