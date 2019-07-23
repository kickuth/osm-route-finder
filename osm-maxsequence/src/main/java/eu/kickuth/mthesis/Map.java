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

    // node locations to draw
    private double[] lats;
    private double[] lons;

    // list of ways, each way is a list of locations
    private List<List<double[]>> wayNodeList;

    // image size
    private int imagePixelWidth = 10000;
    private int imagePixelHeight = 10000;

    public Map(OsmBounds bounds, double[] lats, double[] lons, List<List<double[]>> wayNodeList)
    {
        minLat = bounds.getBottom();
        minLon = bounds.getLeft();
        maxLat = bounds.getTop();
        maxLon = bounds.getRight();
        latExtent = maxLat - minLat;
        lonExtent = maxLon - minLon;


        this.lats = lats;
        this.lons = lons;

        this.wayNodeList = wayNodeList;
    }

    public void writeImage(boolean drawNodes, boolean drawLines)
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

            int bubble_size = 10; // size of each mark in the image

            if (drawNodes) {
                // Draw each node
                for (int i = 0; i < lats.length; i++) {
                    int ly = latToInt(lats[i]);
                    int lx = lonToInt(lons[i]);

                    graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                            bubble_size, bubble_size);
                }
            }

            if (drawLines)
            {
                for (List<double[]> wayNodes : wayNodeList)
                {
                    Iterator<double[]> iter = wayNodes.iterator();
                    double[] fst = iter.next(); // TODO check if nodes exist in this path
                    while (iter.hasNext()) {
                        double[] snd = fst; // TODO check that this copies correctly
                        fst = iter.next(); // TODO check that this copies correctly

                        int y1 = latToInt(fst[0]);
                        int x1 = lonToInt(fst[1]);

                        int y2 = latToInt(snd[0]);
                        int x2 = lonToInt(snd[1]);

                        graphics.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // TODO fix temp file storage path
            ImageIO.write(image, "png", new File("/home/todd/Desktop/roads_lat_lon.png"));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int latToInt(double lat)
    {
        /**
         * compute graphics pixel y-position for a given latitude, knowing image size and lat/lon borders
         */
        // Pixel increases downwards. Latitude increases upwards (north direction). --> inverse mapping.
        return (int) (imagePixelWidth - imagePixelWidth * (lat - minLat) / latExtent);
    }

    private int lonToInt(double lon)
    {
        /**
         * compute graphics pixel x-position for a given longitude, knowing image size and lat/lon borders
         */
        return (int) (imagePixelWidth * (lon - minLon) / lonExtent);
    }
}
