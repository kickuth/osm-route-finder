package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.model.iface.OsmBounds;

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
    private static Color[] colours = {Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.ORANGE, Color.CYAN, Color.PINK};


    public MapRenderer(OsmBounds bounds, Graph g) {
        minLat = bounds.getBottom();
        minLon = bounds.getLeft();
        maxLat = bounds.getTop();
        maxLon = bounds.getRight();
        latExtent = maxLat - minLat;
        lonExtent = maxLon - minLon;

        poiSets = new LinkedList<>();
        graph = g;
    }


    public void writeImage(boolean drawPOIs, boolean drawLines, String fileLocation) {
        if (fileLocation == null || fileLocation.equals("")) {
            fileLocation = System.getProperty("user.home") + "/Desktop/map-export.png";
        }

        System.out.println(String.format("Generating map wth bounds:\nminLat %f, maxLat %f\nminLon %f, maxLon %f",
                minLat, minLon, maxLat, maxLon));
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
            graphics.setColor(Color.BLACK);

            //draw nodes and paths
            Map<String, Color> uniqueTypes = new HashMap<>();
            for (Node fst : graph.adjList.keySet()) {
                // draw nodes with non-empty type/class
                String type = fst.getType();
                if (type != null && !type.equals("")) {
                    Color c = uniqueTypes.get(type);
                    // add new type, colour if type not yet present
                    if (c == null) {
                        c = colours[colourIdx++ % colours.length];
                        uniqueTypes.put(type, c);
                    }
                    int ly = latToPixel(fst.getLat());
                    int lx = lonToPixel(fst.getLon());

                    graphics.setColor(c);
                    graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                            bubble_size, bubble_size);
                    graphics.setColor(Color.BLACK);
                }

                // draw each path
                for (Node snd : graph.adjList.get(fst)) {
                    int y1 = latToPixel(fst.getLat());
                    int x1 = lonToPixel(fst.getLon());

                    int y2 = latToPixel(snd.getLat());
                    int x2 = lonToPixel(snd.getLon());

                    graphics.drawLine(x1, y1, x2, y2);
                }
            }

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
