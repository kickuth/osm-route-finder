package eu.kickuth.mthesis.graph;


import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;

public class Graph {

    private static final Logger logger = LogManager.getLogger(Graph.class);

    public final List<List<Edge>> adjList;
    public final List<List<Edge>> adjListRev;
    public final List<Node> nodes;
    public final Set<Node> pois;
    public final Map<String, Integer> poiTypes = new TreeMap<>(); // TODO private
    private final List<List<Node>> poiGrid;

    // bounds related variables
    private final double[] bounds;  // top/N, bottom/S, left/W, right/E
    private final int nsLineCount;


    public Graph(double[] bounds) {
        this(bounds, 10_000_000);
    }

    public Graph(double[] bounds, int nodeCountEstimate) {
        this.bounds = bounds;
        adjList = new ArrayList<>(nodeCountEstimate);
        adjListRev = new ArrayList<>(nodeCountEstimate);
        nodes = new ArrayList<>(nodeCountEstimate);
        pois = new HashSet<>(nodeCountEstimate / 200);

        // TODO poiGrid based on distances, not lat/lon values
        // Initialise POI grid
        double nsDiff = Math.abs(bounds[0] - bounds[1]);
        double weDiff = Math.abs(bounds[2] - bounds[3]);
        nsLineCount = (int) Math.ceil(nsDiff / POI_GRID_FIDELITY);
        int weLineCount = (int) Math.ceil(weDiff / POI_GRID_FIDELITY);
        poiGrid = new ArrayList<>(nsLineCount * weLineCount);
        for (int i = 0; i < nsLineCount * weLineCount; i++) {
            poiGrid.add(new ArrayList<>());
        }
    }

    /**
     * Add a node to the graph.
     *
     * @param toAdd node to add
     */
    public void addNode(Node toAdd) {
        adjList.add(new ArrayList<>());
        adjListRev.add(new ArrayList<>());
        nodes.add(toAdd);

        // check if the node is a POI
        String nodeType = toAdd.type;
        if (!StringUtils.isEmpty(nodeType)) {
            // add POI
            pois.add(toAdd);
            addToPoiGrid(toAdd);

            // count up respective POI type: increment by one, or set to 1 if not present
            poiTypes.merge(toAdd.type, 1, Integer::sum);
        }
    }

    /**
     * Add POI to the correct element in the POI grid
     * @param poi The node we want to add to the grid
     */
    private void addToPoiGrid(Node poi) {
        double south = poi.lat - bounds[1];
        double west = poi.lon - bounds[2];

        int appropriateField = (int) Math.floor(west / POI_GRID_FIDELITY)  // west/east index
                + nsLineCount * (int) Math.floor(south / POI_GRID_FIDELITY);  // north/south index

        poiGrid.get(appropriateField).add(poi);
    }

    /**
     * Add an edge to the graph
     */
    public void addEdge(Edge edge) {
        try {
            adjList.get(edge.source.id).add(edge);
            adjListRev.get(edge.dest.id).add(edge);
        } catch (IndexOutOfBoundsException e) {
            logger.warn("Edge {} from non existent node {} added. Ignoring.", edge, edge.source);
        }
    }

    /**
     * Get node by id. Returns null if node with specified id is not present in graph.
     * @param id the node's id
     * @return node with specified id, null if no node present
     */
    public Node getNode(int id) {
        try {
            return nodes.get(id);
        } catch (IndexOutOfBoundsException e) {
            logger.error("Non-existent node requested (id: {}).", id);
            return null;
        }
    }

    public Graph createSubgraph(Set<Node> nodeSubset) {
        Graph subGraph = new Graph(bounds, nodeSubset.size());
        // populate subgraph with nodes
        for (Node node : nodeSubset) {
            subGraph.addNode(node);
        }

        // add edges present in both graphs
        for (Node node : nodeSubset) {
            var neighbours = adjList.get(node.id);
            for (Edge neighbour : neighbours) {
                if (nodeSubset.contains(neighbour.dest)) {
                    subGraph.addEdge(neighbour);
                }
            }
        }

        return subGraph;
    }

    public Collection<Node> getPois(double[] area) {
        int north = (int) Math.ceil((area[0] - bounds[1]) / POI_GRID_FIDELITY);
        int south = (int) Math.floor((area[1] - bounds[1]) / POI_GRID_FIDELITY);
        int west = (int) Math.floor((area[2] - bounds[2]) / POI_GRID_FIDELITY);
        int east = (int) Math.ceil((area[3] - bounds[2]) / POI_GRID_FIDELITY);

        List<Node> results = new ArrayList<>();

        // add all elements in all touching grid cells
        for (int we = west; we < east; we++) {
            for (int sn = south; sn < north; sn++) {
                results.addAll(poiGrid.get(we + nsLineCount * sn));
            }
        }

        return results;
    }

    public Set<Node> getPoisOnPath(Path p) {
        return getPoisOnPath(p.getNodes());
    }

    public Set<Node> getPoisOnPath(List<Node> p) {
        return p.stream().filter(pois::contains).collect(Collectors.toSet());
    }

    public List<Node> getOrderedPoisOnPath(Path p) {
        return getOrderedPoisOnPath(p.getNodes());
    }

    public List<Node> getOrderedPoisOnPath(List<Node> p) {
        return p.stream().filter(pois::contains).collect(Collectors.toList());
    }

    public class Path {

        private final LinkedList<DijkstraNode> dNodes;
        private double pathCost;

        Path(LinkedList<DijkstraNode> nodes) {
            this.dNodes = nodes;
            pathCost = (nodes.isEmpty() ? 0 : nodes.getLast().distanceFromSource);
        }

        public Path() {
            dNodes = new LinkedList<>();
            pathCost = 0;
        }

        public Path append(Path toAppend) {
            if (toAppend.dNodes.isEmpty()) {
                // do nothing
            } else if (dNodes.isEmpty()) {
                // copy everything from toAppend
                dNodes.addAll(toAppend.dNodes);
                pathCost = toAppend.pathCost;
            } else if(!dNodes.getLast().node.equals(toAppend.dNodes.getFirst().node)) {
                throw new IllegalArgumentException("Appended path does not start with end node of previous path!");
            } else {
                toAppend.dNodes.forEach(dNode -> dNode.distanceFromSource += pathCost);
                dNodes.removeLast();
                dNodes.addAll(toAppend.dNodes);
                pathCost = dNodes.getLast().distanceFromSource;
            }
            return this;
        }

        public Path insert(Path toInsert, int start, int end) {
            // check if parameters are invalid. Passing this check implies that dNodes.size() > 0.
            if (dNodes.size() <= Math.max(start, end) || Math.min(start, end) < 0) {
                logger.error("Insertion points are out of bounds: start {}, end {}, path length {}", start, end, dNodes.size());
                throw new IllegalArgumentException();
            }

            // LinkedList.subList does not create a copy. Therefore we map dNodes to new DijkstraNodes
            LinkedList<DijkstraNode> backList = new LinkedList<>(dNodes.subList(end, dNodes.size()));
            double reducedCost = backList.getFirst().distanceFromSource;
            Path back = new Path(backList.stream().map(
                    dNode -> new DijkstraNode(dNode.node, dNode.distanceFromSource - reducedCost))
                    .collect(Collectors.toCollection(LinkedList::new)));

            dNodes.subList(start+1, dNodes.size()).clear();
            // if start == end, backList's first node is our last node.
            pathCost = (start == end ? reducedCost : dNodes.getLast().distanceFromSource);

            return append(toInsert).append(back);
        }

        public double getPathCost() {
            return pathCost;
        }

        public LinkedList<Node> getNodes() {
            return dNodes.stream().map(dNode -> dNode.node).collect(Collectors.toCollection(LinkedList::new));
        }

        public Node get(int index) {
            return dNodes.get(index).node;
        }

        public Node getFirst() {
            return dNodes.getFirst().node;
        }

        public Node getLast() {
            return dNodes.getLast().node;
        }

        public boolean isEmpty() {
            return dNodes.isEmpty();
        }

        public int size() {
            return dNodes.size();
        }

        public Path copy() {
            return new Path(dNodes.stream().map(dNode -> new DijkstraNode(dNode.node, dNode.distanceFromSource)).collect(Collectors.toCollection(LinkedList::new)));
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("Path: ");
            if (dNodes.size() < 15) {
                for (DijkstraNode dNode : dNodes) {
                    s.append(dNode.node.id).append(", ");
                }
            } else {
                s.append(String.format("%d, %d, [%d nodes], %d, ", dNodes.getFirst().node.id,
                        dNodes.get(1).node.id, dNodes.size()-3, dNodes.getLast().node.id));
            }
            s.append("Total length: ").append(pathCost);
            return s.toString();
        }
    }

}