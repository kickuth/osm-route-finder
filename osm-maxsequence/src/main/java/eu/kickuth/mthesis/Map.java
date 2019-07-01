package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.model.iface.OsmBounds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Map {

    private double minLon;
    private double minLat;
    private double maxLon;
    private double maxLat;

    // node locations to draw
    private double[] lats;
    private double[] lons;

    // image size
    private int imagePixelWidth = 700;
    private int imagePixelHeight = 700;

    public Map(OsmBounds bounds, double[] lats, double[] lons)
    {
        minLat = bounds.getBottom();
        minLon = bounds.getLeft();
        maxLat = bounds.getTop();
        maxLon = bounds.getRight();

        this.lats = lats;
        this.lons = lons;
    }

    public void writeImage()
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

            double latExtent = maxLat - minLat;
            double lonExtent = maxLon - minLon;

            // Draw each node
            for (int i = 0; i < lats.length; i++)
            {
                double lat = lats[i];
                double lon = lons[i];


                double ly1 = (imagePixelWidth * (lat - minLat)) / latExtent;
                double lx1 = (imagePixelWidth * (lon - minLon)) / lonExtent;

                // Pixel increases downwards. Latitude increases upwards (north direction). --> inverse mapping.
                int ly = (int) (imagePixelWidth - ly1);
                int lx = (int) lx1;

                graphics.fillOval(lx - bubble_size / 2, ly - bubble_size / 2,
                        bubble_size, bubble_size);
            }

            // TODO fix temp file storage path
            ImageIO.write(image, "png", new File("/home/todd/Desktop/road_sign_lat_lon.png"));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
