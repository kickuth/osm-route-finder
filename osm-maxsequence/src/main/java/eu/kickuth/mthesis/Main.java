package eu.kickuth.mthesis;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException
    {
        // Open dump file as stream
        InputStream input = null;
        try
        {
            input = ClassLoader.getSystemClassLoader().getResource("./osm_data/tue.osm.pbf").openStream();
        } catch (NullPointerException e)
        {
            System.out.println("Failed to locate map dump!");
            System.exit(1);
        }

        // Create a reader for PBF data
        OsmIterator iterator = new PbfIterator(input, true);


        // Iterate contained entities
        for (EntityContainer container : iterator)
        {

            // Only use nodes
            if (container.getType() == EntityType.Node)
            {

                // Get the node from the container
                OsmNode node = (OsmNode) container.getEntity();

                // Convert the node's tags to a map
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);

                // Get the value for the 'amenity' key
                String amenity = tags.get("amenity");

                // Check if this is a restaurant
                boolean isRestaurant = amenity != null
                        && amenity.equals("restaurant");

                // If yes, print name and coordinate
                if (isRestaurant)
                {
                    System.out.println(String.format("%s: %f, %f",
                            tags.get("name"),
                            node.getLatitude(),
                            node.getLongitude()
                    ));
                }
            }
        }
    }
}
