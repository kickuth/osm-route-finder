package eu.kickuth.mthesis.graph;


import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static eu.kickuth.mthesis.utils.Settings.*;

public class Graph {

    public Map<Node, Set<Node>> adjList;
    private Map<Long, Node> nodes;
    private Set<Node> pois;
    public HashMap<String, Integer> poiTypes = new HashMap<>(); // TODO private
    private List<List<Node>> poiGrid;

    // bounds related variables
    private final double[] bounds;  // top/N, bottom/S, left/W, right/E
    private final int nsLineCount;


    public Graph(double[] bounds) {
        this(bounds, 1_000_000);
    }

    public Graph(double[] bounds, int nodeCountEstimate) {
        this.bounds = bounds;
        adjList = new HashMap<>(nodeCountEstimate + nodeCountEstimate / 3);
        nodes = new HashMap<>(nodeCountEstimate + nodeCountEstimate / 3);
        pois = new HashSet<>(nodeCountEstimate / 100);

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
     * @return false, iff the node already was present.
     */
    public boolean addNode(Node toAdd) {
        if (adjList.containsKey(toAdd)) {
            return false;
        } else {
            adjList.put(toAdd, new HashSet<>());
            nodes.put(toAdd.getId(), toAdd);

            // check if the node is a POI
            String nodeType = toAdd.getType();
            if (!StringUtils.isEmpty(nodeType)) {
                // add POI
                pois.add(toAdd);
                addToPoiGrid(toAdd);

                // count up respective POI type: increment by one, or set to 1 if not present
                poiTypes.merge(toAdd.getType(), 1, Integer::sum);
            }
            return true;
        }
    }

    /**
     * Add POI to the correct element in the POI grid
     * @param poi The node we want to add to the grid
     */
    private void addToPoiGrid(Node poi) {
        double south = poi.getLat() - bounds[1];
        double west = poi.getLon() - bounds[2];

        int appropriateField = (int) Math.floor(west / POI_GRID_FIDELITY)  // west/east index
                + nsLineCount * (int) Math.floor(south / POI_GRID_FIDELITY);  // north/south index

        poiGrid.get(appropriateField).add(poi);
    }

    /**
     * Add an edge to the graph
     *
     * @param source edge source
     * @param dest   edge destination
     * @return false, iff the edge already exists.
     * @throws IllegalArgumentException, if one of the nodes is not present in the graph
     */
    public boolean addEdge(Node source, Node dest) {
        // assure that the nodes exist in the graph
        if (adjList.get(source) == null || adjList.get(dest) == null) {
            throw new IllegalArgumentException("Nodes not present in graph!");
        }
        // check if the edge already exists
        Set<Node> sourceNeighbours = adjList.get(source);
        if (sourceNeighbours.contains(dest)) {
            return false;
        }
        // add edge
        sourceNeighbours.add(dest);
        return true;
    }

    public Node getNode(long id) {
        return nodes.get(id);
    }

    public Graph createSubgraph(Set<Node> nodeSubset) {
        Graph subGraph = new Graph(bounds, nodeSubset.size());
        // populate subgraph with nodes
        for (Node node : nodeSubset) {
            subGraph.addNode(node);
        }

        // add edges present in both graphs
        for (Node node : nodeSubset) {
            Set<Node> neighbours = adjList.get(node);
            for (Node neighbour : neighbours) {
                if (nodeSubset.contains(neighbour)) {
                    subGraph.addEdge(node, neighbour);
                }
            }
        }

        return subGraph;
    }

    public Collection<Node> getPois() {
        return pois;
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
        return p.stream().filter(node -> pois.contains(node)).collect(Collectors.toSet());
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
                return this;
            }
            if (dNodes.isEmpty()) {
                // copy everything from toAppend
                dNodes.addAll(toAppend.dNodes);
                pathCost = toAppend.pathCost;
                return this;
            }
            if(!dNodes.getLast().node.equals(toAppend.dNodes.getFirst().node)) {
                throw new IllegalArgumentException("Appended path does not start with end node of previous path!");
            }
            toAppend.dNodes.forEach(dNode -> dNode.distanceFromSource += pathCost);
            dNodes.removeLast();
            dNodes.addAll(toAppend.dNodes);
            pathCost = dNodes.getLast().distanceFromSource;
            return this;
        }

        public Path insert(Path toInsert, int start, int end) {
            // check if parameters are invalid. Passing this check implies that dNodes.size() > 0.
            if (dNodes.size() <= Math.max(start, end) || Math.min(start, end) < 0) {
                throw new IllegalArgumentException("Insertion points are out of bounds!");
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

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("Path: ");
            if (dNodes.size() < 15) {
                for (DijkstraNode dNode : dNodes) {
                    s.append(dNode.node.getId()).append(", ");
                }
            } else {
                s.append(String.format("%d, %d, [%d nodes], %d, ", dNodes.getFirst().node.getId(),
                        dNodes.get(1).node.getId(), dNodes.size()-3, dNodes.getLast().node.getId()));
            }
            s.append("Total length: ").append(pathCost);
            return s.toString();
        }
    }

}
