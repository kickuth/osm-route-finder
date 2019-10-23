package eu.kickuth.mthesis.utils;

import crosby.binary.osmosis.OsmosisReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;

import static eu.kickuth.mthesis.utils.Settings.*;


public class OSMReader implements Sink {

    private static final Logger logger = LogManager.getLogger(OSMReader.class);

    private HashSet<eu.kickuth.mthesis.graph.Node> nodes = new HashSet<>();


    @Override
    public void initialize(Map<String, Object> metaData) {

    }

    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer instanceof NodeContainer) {
            Node OSMNode = ((NodeContainer) entityContainer).getEntity();
            String type = null;
            for (Tag tag : OSMNode.getTags()) {
                if ("traffic_sign".equalsIgnoreCase(tag.getKey())) {
                    type = tag.getValue();
                    break;
                }
            }
             nodes.add(new eu.kickuth.mthesis.graph.Node(
                    OSMNode.getId(), OSMNode.getLatitude(), OSMNode.getLongitude(), type));
            // TODO
        } else if (entityContainer instanceof WayContainer) {
            Way way = ((WayContainer) entityContainer).getEntity(); // TODO
            for (Tag tag : way.getTags()) {
                if ("highway".equalsIgnoreCase(tag.getKey())) {
                    System.out.println(" Woha, it's a highway: " + way.getId());
                    break;
                }
            }
        } else if (entityContainer instanceof RelationContainer) {
            // We don't process relations
        } else {
            logger.warn("Unknown Entity: {}", entityContainer.getEntity());
        }
    }

    @Override
    public void complete() {

    }

    @Override
    public void close() {

    }

    public static void main(String[] args) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(OSM_DUMP);
        OsmosisReader reader = new OsmosisReader(inputStream);
        reader.setSink(new OSMReader());
        reader.run();
    }
}
