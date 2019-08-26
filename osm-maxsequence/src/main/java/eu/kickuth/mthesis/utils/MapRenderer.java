package eu.kickuth.mthesis.utils;

import de.topobyte.osm4j.core.model.iface.OsmBounds;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapRenderer {

    private double minLon;
    private double minLat;
    private double maxLon;
    private double maxLat;
    private double latExtent;
    private double lonExtent;

    // List of POI locations
    private List<List<double[]>> poiSets;

    // The graph to draw
    private Graph graph;

    // image size
    private int imagePixelWidth = 5000;  // TODO hard-coded image size
    private int imagePixelHeight = 5000;
    private static Color[] colours = {Color.RED, Color.BLUE, Color.WHITE, Color.GREEN, Color.CYAN, Color.YELLOW,
            Color.PINK, new Color(125, 50, 40) /* brown */};

    public MapRenderer(Graph g) {
        minLon = 180;
        minLat = 90;
        maxLon = -180;
        maxLat = -90;
        for (Node node : g.adjList.keySet()) {
            double lat = node.getLat();
            double lon = node.getLon();
            if (minLat > lat) {
                minLat = lat;
            } else if (maxLat < lat) {
                maxLat = lat;
            }
            if (minLon > lon) {
                minLon = lon;
            } else if (maxLon < lon) {
                maxLon = lon;
            }
        }
        graph = g;

        latExtent = maxLat - minLat;
        lonExtent = maxLon - minLon;
        poiSets = new LinkedList<>();
    }

    public MapRenderer(Graph g, OsmBounds b) {
        this(g, b.getBottom(), b.getLeft(), b.getTop(), b.getRight());
    }

    public MapRenderer(Graph g, double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
        graph = g;

        latExtent = maxLat - minLat;
        lonExtent = maxLon - minLon;
        poiSets = new LinkedList<>();
    }


    public void writeImage(String fileLocation) {
        if (StringUtils.isEmpty(fileLocation)) {
            fileLocation = System.getProperty("user.home") + "/Desktop/map-export.png";
        }
        System.out.println(String.format("Generating map with bounds:\nLatitude (%f, %f)\nLongitude (%f, %f)",
                minLat, maxLat, minLon, maxLon));
        try {
            // create image with background
            BufferedImage image = new BufferedImage(imagePixelWidth,
                    imagePixelHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.fillRect(0, 0, imagePixelWidth, imagePixelHeight);
            graphics.setColor(Color.BLACK);

            int bubble_size = 10; // TODO hard-coded pixel size of each POI/Node in the image

            // draw each POI
            int colourIdx = 0;
            for (List<double[]> poiSet : poiSets) {
                // change colour for each POI class
                graphics.setColor(colours[colourIdx++ % colours.length]);

                for (double[] poi : poiSet) {
                    if (poi[0] < minLat || poi[0] > maxLat || poi[1] < minLon || poi[1] > maxLon) {
                        System.err.println(String.format(
                                "Ignoring POI outside of map bounds: (%f, %f)", poi[0], poi[1]));
                        continue;
                    }
                    int ly = latToPixel(poi[0]);
                    int lx = lonToPixel(poi[1]);

                    graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                            bubble_size, bubble_size);
                }
            }

            // draw nodes with non-empty type/class
            Map<String, Color> uniqueTypes = new HashMap<>();
            for (Node node : graph.adjList.keySet()) {
                String type = node.getType();
                if (!StringUtils.isEmpty(type)) {
                    Color c = uniqueTypes.get(type);
                    // if type not yet present, add new type with colour
                    if (c == null) {
                        c = colours[colourIdx++ % colours.length];
                        uniqueTypes.put(type, c);
                    }
                    int ly = latToPixel(node.getLat());
                    int lx = lonToPixel(node.getLon());

                    graphics.setColor(c);
                    graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                            bubble_size, bubble_size);
                }
            }
            System.out.println("Number of unique node types/classes: " + uniqueTypes.size());

            // draw all edges
            graphics.setColor(Color.BLACK);
            for (Node fst : graph.adjList.keySet()) {
                for (Node snd : graph.adjList.get(fst)) {
                    int y1 = latToPixel(fst.getLat());
                    int x1 = lonToPixel(fst.getLon());

                    int y2 = latToPixel(snd.getLat());
                    int x2 = lonToPixel(snd.getLon());

                    graphics.drawLine(x1, y1, x2, y2);
                }
            }

            System.out.println("Number of colour classes: " + colourIdx);

            // write image to disk
            ImageIO.write(image, "png", new File(fileLocation));
            System.out.println("Image written to " + fileLocation);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a class of (non-Node) POIs by latitude and longitude.
     * @param poiSet list of POIs to add
     */
    public void addPOISet(List<double[]> poiSet) {
        poiSets.add(poiSet);
    }

    /**
     * Remove all (non-Node) POIs
     */
    public void clearPois() {
        poiSets.clear();
    }


    /**
     * compute graphics pixel y-position for a given latitude, knowing image size and lat/lon borders
     */
    private int latToPixel(double lat) {
        // Pixel increases downwards. Latitude increases upwards (north direction).
        return (int) (imagePixelWidth - imagePixelWidth * (lat - minLat) / latExtent);
    }


    /**
     * compute graphics pixel x-position for a given longitude, knowing image size and lat/lon borders
     */
    private int lonToPixel(double lon) {
        return (int) (imagePixelWidth * (lon - minLon) / lonExtent);
    }
}
