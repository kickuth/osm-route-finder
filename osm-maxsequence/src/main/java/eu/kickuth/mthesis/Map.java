package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.model.iface.OsmBounds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Map {

    private double minLon;
    private double minLat;
    private double maxLon;
    private double maxLat;
    private double latExtent;
    private double lonExtent;

    // List of POI locations
    private List<double[]> POIs;

    // list of ways, each way is a list of locations
    private List<List<double[]>> wayNodeList;

    // image size
    private int imagePixelWidth = 10000;
    private int imagePixelHeight = 10000;

    public Map(OsmBounds bounds, List<double[]> POIs, List<List<double[]>> wayNodeList)
    {
        minLat = bounds.getBottom();
        minLon = bounds.getLeft();
        maxLat = bounds.getTop();
        maxLon = bounds.getRight();
        latExtent = maxLat - minLat;
        lonExtent = maxLon - minLon;

        this.POIs = POIs;
        this.wayNodeList = wayNodeList;
    }

    public void writeImage(boolean drawPOIs, boolean drawLines)
    {
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

            if (drawPOIs)
            {
                // draw each POI
                for (double[] POI : POIs) {
                    int ly = latToPixel(POI[0]);
                    int lx = lonToPixel(POI[1]);

                    graphics.setColor(Color.RED);
                    graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                            bubble_size, bubble_size);
                    graphics.setColor(Color.BLACK);
                }
            }

            if (drawLines)
            {
                // draw each path
                for (List<double[]> wayNodes : wayNodeList)
                {
                    if (wayNodes.isEmpty()) {
                        // skip empty paths
                        continue;
                    }
                    Iterator<double[]> iter = wayNodes.iterator();
                    double[] fst = iter.next();
                    while (iter.hasNext()) {
                        double[] snd = fst;
                        fst = iter.next();

                        int y1 = latToPixel(fst[0]);
                        int x1 = lonToPixel(fst[1]);

                        int y2 = latToPixel(snd[0]);
                        int x2 = lonToPixel(snd[1]);

                        graphics.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // write image to disk
            // TODO fix temp file storage path
            ImageIO.write(image, "png", new File("/home/todd/Desktop/roads_lat_lon.png"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * compute graphics pixel y-position for a given latitude, knowing image size and lat/lon borders
     */
    private int latToPixel(double lat)
    {
        // Pixel increases downwards. Latitude increases upwards (north direction). --> inverse mapping.
        return (int) (imagePixelWidth - imagePixelWidth * (lat - minLat) / latExtent);
    }


    /**
     * compute graphics pixel x-position for a given longitude, knowing image size and lat/lon borders
     */
    private int lonToPixel(double lon)
    {

        return (int) (imagePixelWidth * (lon - minLon) / lonExtent);
    }
}
