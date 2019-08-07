package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.model.iface.OsmBounds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MapRenderer {

    private double minLon;
    private double minLat;
    private double maxLon;
    private double maxLat;
    private double latExtent;
    private double lonExtent;

    // List of POI locations
    private List<double[]> POIs;

    // The graph to draw
    private Graph graph;

    // image size
    private int imagePixelWidth = 5000;
    private int imagePixelHeight = 5000;


    public MapRenderer(OsmBounds bounds, List<double[]> POIs, Graph g) {
        minLat = bounds.getBottom();
        minLon = bounds.getLeft();
        maxLat = bounds.getTop();
        maxLon = bounds.getRight();
        latExtent = maxLat - minLat;
        lonExtent = maxLon - minLon;

        this.POIs = POIs;
        graph = g;
    }


    public void writeImage(boolean drawPOIs, boolean drawLines, String fileLocation) {
        if (fileLocation == null || fileLocation.equals("")) {
            fileLocation = "/home/todd/Desktop/roads_lat_lon.png";
        }

        try {
            BufferedImage image = new BufferedImage(imagePixelWidth,
                    imagePixelHeight, BufferedImage.TYPE_INT_ARGB);

            System.out.println(String.format("minLat %f\nminLon %f\nmaxLat %f\nmaxLon %f",
                    minLat, minLon, maxLat, maxLon));

            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, imagePixelWidth, imagePixelHeight);
            graphics.setColor(Color.BLACK);

            int bubble_size = 10; // TODO hardcoded: Size of each mark in the image

            if (drawPOIs) {  // draw each POI
                for (double[] POI : POIs) {
                    int ly = latToPixel(POI[0]);
                    int lx = lonToPixel(POI[1]);

                    graphics.setColor(Color.RED);
                    graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                            bubble_size, bubble_size);
                    graphics.setColor(Color.BLACK);
                }
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

    public void setPOIs(List<double[]> POIs) {
        this.POIs = POIs;
    }


    /**
     * compute graphics pixel y-position for a given latitude, knowing image size and lat/lon borders
     */
    private int latToPixel(double lat) {
        // Pixel increases downwards. Latitude increases upwards (north direction). --> inverse mapping.
        return (int) (imagePixelWidth - imagePixelWidth * (lat - minLat) / latExtent);
    }


    /**
     * compute graphics pixel x-position for a given longitude, knowing image size and lat/lon borders
     */
    private int lonToPixel(double lon) {
        return (int) (imagePixelWidth * (lon - minLon) / lonExtent);
    }
}
