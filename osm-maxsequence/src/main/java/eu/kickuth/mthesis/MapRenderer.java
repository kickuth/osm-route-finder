package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.model.iface.OsmBounds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
    private static Color[] colours = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE, Color.CYAN, Color.PINK};


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

        try {
            BufferedImage image = new BufferedImage(imagePixelWidth,
                    imagePixelHeight, BufferedImage.TYPE_INT_ARGB);

            System.out.println(String.format("minLat %f\nminLon %f\nmaxLat %f\nmaxLon %f",
                    minLat, minLon, maxLat, maxLon));

            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.fillRect(0, 0, imagePixelWidth, imagePixelHeight);
            graphics.setColor(Color.BLACK);

            int bubble_size = 10; // TODO hard-coded size of each POI in the image

            if (drawPOIs) {  // draw each POI
                int colourIdx = 0;
                for (List<double[]> poiSet : poiSets) {
                    // change colour for each POI class
                    graphics.setColor(colours[colourIdx++ % colours.length]);

                    for (double[] poi : poiSet) {
                        int ly = latToPixel(poi[0]);
                        int lx = lonToPixel(poi[1]);

                        graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                                bubble_size, bubble_size);
                    }
                }
                graphics.setColor(Color.BLACK);
            }

            if (drawLines) {  // draw each path
                for (Node fst : graph.adjList.keySet()) {
                    for (Node snd : graph.adjList.get(fst)) {
                        int y1 = latToPixel(fst.getLat());
                        int x1 = lonToPixel(fst.getLon());

                        int y2 = latToPixel(snd.getLat());
                        int x2 = lonToPixel(snd.getLon());

                        graphics.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // write image to disk
            ImageIO.write(image, "png", new File(fileLocation));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a class of POIs
     * @param poiSet list of POIs to add
     */
    public void addPOISet(List<double[]> poiSet) {
        poiSets.add(poiSet);
    }

    /**
     * Remove all POIs
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
