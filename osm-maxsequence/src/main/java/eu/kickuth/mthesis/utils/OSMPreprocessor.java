package eu.kickuth.mthesis.utils;

import crosby.binary.osmosis.OsmosisReader;
import crosby.binary.osmosis.OsmosisSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.Source;
import org.openstreetmap.osmosis.osmbinary.file.BlockOutputStream;
//import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;

public class OSMPreprocessor implements Sink, Source {

    private static final Logger logger = LogManager.getLogger(OSMPreprocessor.class);

    // pattern to recognize real road signs
    private final Pattern roadSignPattern = Pattern.compile("(DE:\\d+)|(city_limit)");

    // default user and date for processed OSM file
    private static final OsmUser user = new OsmUser(0, "");
    private static final Date date = new Date(0L);

    // repeatable random numbers for fake (and augmented) classes
    private final Random random = new Random(0);

    // data writer
    private Sink sink;

    private final Set<Long> nodesOnRoads;
    private final Set<Long> nodesOnJunctions;

    // node distribution (used as input for fake traffic sign generation)
    private int[][] nodesDistribution = null;
    private long nodesWeightedTotal = 0;  // used for clustered distribution
    private ArrayList<Node> processQueue = new ArrayList<>();
    private double[] bounds;

    private final Map<Long, Integer> idMap;
    private int mappedID = 0;

    private final File outputFile;


    public OSMPreprocessor(final File outputFile) throws FileNotFoundException {
        logger.trace("First run: Finding node IDs on paths");
        this.outputFile = outputFile;
        OSMNodesOnPathReader nodesReader = new OSMNodesOnPathReader();
        InputStream inputStream = new FileInputStream(OSM_DUMP);
        OsmosisReader reader = new OsmosisReader(inputStream);
        reader.setSink(nodesReader);
        reader.run();
        nodesOnRoads = nodesReader.getNodeIDs();
        nodesOnJunctions = nodesReader.getJunctionIDs();
        idMap = new HashMap<>((nodesOnRoads.size() * 4) / 3);
    }


    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    @Override
    public void initialize(Map<String, Object> metaData) {
        logger.trace("Second run: Preprocessing");
        // initialise writer
        try {
//            setSink(new XmlWriter(new BufferedWriter(new FileWriter(outputFile))));  // xml writer
            setSink(new OsmosisSerializer(new BlockOutputStream(new FileOutputStream(outputFile))));  // pbf writer
        } catch (FileNotFoundException e) {
            logger.fatal("Could not find File!", e);
//        } catch (IOException e) {  // for FileWriter in XmlWriter
//            logger.fatal("Error using FileWriter!", e);
        }
    }

    @Override
    public void process(EntityContainer container) {
        if (container instanceof NodeContainer) {
            processNode((Node) container.getEntity());

        } else if (container instanceof WayContainer) {
            processWay((Way) container.getEntity());

        } else if (container instanceof RelationContainer) {
            // We don't process relations

        } else if (container instanceof BoundContainer) {
            sink.process(container);  // store bounds as are.

            // If we are generating fake traffic signs based on density, we initialise the node distribution array

            // skip node distribution array initialisation if not needed for fake classes
            if (!GENERATE_FAKE_SIGNS) {
                return;
            }

            Bound b = ((BoundContainer) container).getEntity();
            bounds = new double[] {b.getTop(), b.getBottom(), b.getLeft(), b.getRight()};

            // check whether we already encountered a bounds container for some reason
            if (nodesDistribution != null) {
                logger.error("Node distribution array already set! Does the file contain multiple bounds? Ignoring.");
                return;
            }
            // initialise node distribution array
            double nsDiff = Math.abs(bounds[0] - bounds[1]);
            double weDiff = Math.abs(bounds[2] - bounds[3]);
            // used to find correct nodesDistribution cell to increment for each node
            int nsLineCount = (int) Math.ceil(nsDiff / NODE_DISTRIBUTION_GRID_FIDELITY);
            int weLineCount = (int) Math.ceil(weDiff / NODE_DISTRIBUTION_GRID_FIDELITY);
            nodesDistribution = new int[nsLineCount][weLineCount];

        } else {
            logger.warn("Unknown Entity: {}", container.getEntity());
        }
    }

    private void processNode(Node osmNode) {
        Long nodeID = osmNode.getId();
        if (!nodesOnRoads.contains(nodeID)) {
            // continue if id was not present
            return;
        }
        idMap.put(nodeID, mappedID);  // store node id mapping for later WayNode lookups

        Collection<Tag> tags = new ArrayList<>(1);
        if (GENERATE_FAKE_SIGNS) {
            // increment correct location in node distribution array, generate sign after all nodes are processed

            // use min/max to "clamp to bounds" in case the node is outside the bounds
            double south = Math.min(Math.max(osmNode.getLatitude() - bounds[1], 0), bounds[0] - bounds[1]);
            double west = Math.min(Math.max(osmNode.getLongitude() - bounds[2], 0), bounds[3] - bounds[2]);

            int ns = (int) Math.floor(south / NODE_DISTRIBUTION_GRID_FIDELITY);
            int we = (int) Math.floor(west / NODE_DISTRIBUTION_GRID_FIDELITY);
            nodesDistribution[ns][we]++;
            nodesWeightedTotal += nodesDistribution[ns][we] * 2 - 1;
        } else {
            // process real (but augmented) traffic sign
            for (Tag tag : osmNode.getTags()) {
                if ("traffic_sign".equalsIgnoreCase(tag.getKey())) {
//                    Pattern roadSignPattern = Pattern.compile("(DE:\\d+)");
                    Matcher matcher = roadSignPattern.matcher(tag.getValue());
                    if (matcher.find()) {
                        String type = matcher.group(0);
                        // we use some of the city_limit POIs as fake classes and ignore the others.
                        if (type.equals("city_limit")) {
                            // get a random number, between 0 and 90
                            int randomInt = random.nextInt(91);
                            if (randomInt >= 65) {  // A=65 to Z=90
                                type = String.valueOf((char) randomInt);
                            } else {
                                // the dice have rolled and this is a sign we will not replace (and not keep)
                                continue;
                            }
                        }
                        tags.add(new Tag("traffic_sign", type));
                    }
                    while (matcher.find()) {
                        // TODO do not ignore? extend regex?
                        logger.info("Ignoring additional sign: {}", matcher.group(0));
                    }
                }
            }
        }

        // check whether the node is a junction and hence should be tagged as such
        if (nodesOnJunctions.contains(nodeID)) {
            tags.add(new Tag("is_junction", "yes"));
        }

        // add updated node to process queue
        processQueue.add(new Node(new CommonEntityData(
                mappedID++, 1, date, user, 0, tags),
                osmNode.getLatitude(), osmNode.getLongitude()
        ));
    }

    private boolean isFirstWay = true;
    private void processWay(Way osmWay) {
        if (isFirstWay) {  // true: we have just finished reading nodes
            isFirstWay = false;

            // generate fake traffic signs (and write nodes to sink) before ways are processed.
            writeNodes();
        }

        boolean isHighway = false;  // is road drivable?
        boolean isOneWay = false;
        Collection<Tag> newTags = new ArrayList<>();

        // filter for useful roads and check if the way is one way only
        for (Tag wayTag : osmWay.getTags()) {
            switch (wayTag.getKey().toLowerCase(Locale.ENGLISH)) {
                case "highway":  // is it a (probably) drivable road?
                    String rt = wayTag.getValue();
                    if (!(rt.startsWith("motorway") || rt.startsWith("trunk") ||
                            rt.startsWith("primary") || rt.startsWith("secondary") || rt.startsWith("tertiary") ||
                            rt.equals("unclassified") || rt.equals("residential"))) {
                        return;
                    }
                    newTags.add(new Tag("highway", rt));
                    isHighway = true;
                    break;
                case "oneway":  // is it explicitly one directional?
                    if ("yes".equalsIgnoreCase(wayTag.getValue()) && !isOneWay) {
                        isOneWay = true;
                        newTags.add(new Tag("oneway", "yes"));
                    }
                    break;
                case "junction":  // is it a roundabout (implies one directional)?
                    if ("roundabout".equalsIgnoreCase(wayTag.getValue()) && !isOneWay) {
                        isOneWay = true;
                        newTags.add(new Tag("oneway", "yes"));
                    }
                    break;
                case "access":  // can we access the road?
                    if ("no".equalsIgnoreCase(wayTag.getValue()) || "private".equalsIgnoreCase(wayTag.getValue())) {
                        return;
                    }
                    break;
                default:
                    // ignore all other tags
                    // TODO include maxspeed?
            }
        }
        if (!isHighway) {
            // way is not a road
            return;
        }

        List<WayNode> newWayNodes = osmWay.getWayNodes().stream().map(
                // note that the library only stores WayNode IDs and getLatitude, getLongitude will always return 0...
                n -> new WayNode(idMap.get(n.getNodeId()), n.getLatitude(), n.getLongitude())
        ).collect(Collectors.toList());

        Way newWay = new Way(new CommonEntityData(osmWay.getId(), 1, date, user, 0, newTags), newWayNodes);

        sink.process(new WayContainer(newWay));
    }

    private void writeNodes() {
        if (GENERATE_FAKE_SIGNS) {
            // log POI distribution grid
            logger.debug("Road distribution for POI class generation grid:\n" +
                    Arrays.deepToString(nodesDistribution)
            );
            logger.info("Generating fake classes:\n" +
                    "{}x{} node cells\n" +
                    "{} nodes total\n" +
                    "{} possible fake classes\n" +
                    "{} expected POIs", nodesDistribution.length, nodesDistribution[0].length, processQueue.size(), EXPECTED_DISTINCT_CLASSES, EXPECTED_TOTAL_POIS);

            // determine and process nodes that will not get a traffic sign
            int nodeCount = processQueue.size();
            if (CLUSTERED_DISTRIBUTION) {
                processQueue.removeIf(osmNode -> {
                    // nodes in dense regions are more likely to be a POI
                    if (random.nextDouble() > ((double) EXPECTED_TOTAL_POIS) * getCellCount(osmNode) / nodesWeightedTotal) {
                        sink.process(new NodeContainer(osmNode));
                        return true;
                    }
                    return false;
                });
            } else {
                processQueue.removeIf(osmNode -> {
                    // uniformly distributed POI chance per node
                    if (random.nextDouble() > EXPECTED_TOTAL_POIS / (double) nodeCount) {
                        sink.process(new NodeContainer(osmNode));
                        return true;
                    }
                    return false;
                });
            }

            // TODO instead of filtering nodes, it would be nice to shuffle them here, accounting for their clustered distribution property
//            // determine nodes that will get a traffic sign by shuffling and then picking nodes from the front.
//            if (CLUSTERED_DISTRIBUTION) {  // road segments in dense areas are more likely to move to the front
//                // include a bias for shuffling
//                processQueue.sort(Comparator.comparingDouble(n -> random.nextDouble() + EXPECTED_TOTAL_POIS * getCellCount(n) / (double) nodesWeightedTotal));
//            }

            int actualTotalPois = processQueue.size();
            logger.info("Actual number of POIs: {}", actualTotalPois);

            // shuffle list, so that we can just pick the first n elements as a random, unique subset
            Collections.shuffle(processQueue);//, new SecureRandom());  // secure random for more possible permutations

            // set correct sampling function
            Function<Void, Integer> randDistr = null;
            double mean = actualTotalPois / (double) EXPECTED_DISTINCT_CLASSES;
            switch (POI_DISTRIBUTION) {
                case "gaussian":
                case "normal":
                    // normal distribution with mean (mu) and standard deviation sigma
                    double sigma = mean / 3; // most samples are > 0.
                    randDistr = (v) -> {
                        int result;
                        do {  // loop to avoid negative and extreme results
                            result = (int) Math.round(random.nextGaussian() * sigma + mean);
                        } while (result < 0 || result > mean * 3);
                        return result;
                    };
                    logger.debug("Generating normally distributed sign counts per class: mean={}, sigma={}", mean, sigma);
                    break;
                case "exp high":
                    // exponential distribution (mostly low, few extremely high)
                    randDistr = (v) -> {
                        int result;
                        do {  // loop to avoid too extreme results (slightly increases number of expected classes)
                            result = (int) Math.round(-Math.log(1-random.nextDouble())*mean);
                        } while (result > mean * 4);
                        return result;
                    };
                    logger.debug("Generating exponentially distributed sign counts per class (few very common classes): lambda={}", mean);
                    break;
                case "exp low":
                    // TODO what distribution? Change log output wording
                    final double log2 = Math.log(2);
                    randDistr = (v) -> {
                        double rand = random.nextDouble();
                        return (int) Math.round(-rand*mean/(log2*Math.log(1-rand)));
                    };
                    logger.debug("Generating exponentially distributed sign counts per class (few very rare classes): lambda={}", mean);
                    break;
                case "linear":
                    // linear distribution
                    randDistr = (v) -> (int) Math.round(random.nextDouble() * mean * 2);
                    logger.debug("Generating linearly distributed sign counts per class: mean={}", mean);
                    break;
                default:
                    logger.error("Unknown POI distribution: {}. Not generating signs.", POI_DISTRIBUTION);
                    processQueue.forEach(osmNode -> sink.process(new NodeContainer(osmNode)));
            }

            // assign POIs until none are left
            ListIterator<Node> queueIterator = processQueue.listIterator();
            int remainingPois = actualTotalPois;
            int currentclass = 1;
            do {
                int poisNextClass = randDistr.apply(null);  // get the next random number
                remainingPois -= poisNextClass;

                if (remainingPois < 0) {  // ensure we don't use too many POIs (queIterator would run out)
                    poisNextClass += remainingPois;
                }

                for (int i = 0; i < poisNextClass; i++) {
                    Node current = queueIterator.next();

                    current.getTags().add(new Tag("traffic_sign", "FC " + String.format("%03d", currentclass)));
                    sink.process(new NodeContainer(current));
                }
                currentclass++;
            } while (remainingPois > 0);
        } else {
            processQueue.forEach(osmNode -> sink.process(new NodeContainer(osmNode)));
        }

    }

    private int getCellCount(Node osmNode) {
        // use min/max to "clamp to bounds" in case the node is outside the bounds
        double south = Math.min(Math.max(osmNode.getLatitude() - bounds[1], 0), bounds[0] - bounds[1]);
        double west = Math.min(Math.max(osmNode.getLongitude() - bounds[2], 0), bounds[3] - bounds[2]);

        int ns = (int) Math.floor(south / NODE_DISTRIBUTION_GRID_FIDELITY);
        int we = (int) Math.floor(west / NODE_DISTRIBUTION_GRID_FIDELITY);
        return nodesDistribution[ns][we];
    }

    /**
     * This method is called, once the input file has been completely read.
     */
    @Override
    public void complete() {
        sink.complete();  // write out remaining output buffer
    }

    @Override
    public void close() {

    }
}