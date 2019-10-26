package eu.kickuth.mthesis.utils;

import crosby.binary.osmosis.OsmosisSerializer;
import eu.kickuth.mthesis.graph.Graph;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.Source;
import org.openstreetmap.osmosis.osmbinary.file.BlockOutputStream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class OSMWriter implements Source {

    private static final Logger logger = LogManager.getLogger(OSMWriter.class);

    private Sink sink;

    public void export(Graph g, String fileLocation) {
        // initialise
        try {
            setSink(new OsmosisSerializer(new BlockOutputStream(new FileOutputStream(fileLocation))));
        } catch (FileNotFoundException e) {
            logger.error("Could not find File! Aborting export.", e);
            return;
        }

        // write nodes
        for (eu.kickuth.mthesis.graph.Node node : g.adjList.keySet()) {
            sink.process(new NodeContainer(new Node(createEntity(node), node.getLat(), node.getLon())));
        }

        // TODO write ways

        sink.complete();
    }

    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    private CommonEntityData createEntity(eu.kickuth.mthesis.graph.Node node) {
        String nodeType = node.getType();
        if (!StringUtils.isEmpty(nodeType)) {
            Collection<Tag> tags = new ArrayList<Tag>(1);
            tags.add(new Tag("Type", nodeType));
            return new CommonEntityData(node.getId(), 1, new Date(), new OsmUser(0, ""), 0, tags);
        } else {
            return new CommonEntityData(node.getId(), 1, new Date(), new OsmUser(0, ""), 0);
        }
    }
}
