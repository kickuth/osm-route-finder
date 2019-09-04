package eu.kickuth.mthesis.graph;

import java.util.*;
import java.util.stream.Collectors;

public class Graph {

    public Map<Node, Set<Node>> adjList;
    private Map<Long, Node> nodes;

    public Graph(Map<Long, Node> nodes, Map<Node, Set<Node>> adjList) {
        this.nodes = nodes;
        this.adjList = adjList;
    }

    public Graph() {
        this(10000);
    }

    public Graph(int nodeCountEstimate) {
        adjList = new HashMap<>(nodeCountEstimate + nodeCountEstimate / 3);
        nodes = new HashMap<>(nodeCountEstimate + nodeCountEstimate / 3);
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
            return true;
        }
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
        Graph subGraph = new Graph(nodeSubset.size());
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

    /**
     * create a deep copy of the graph
     * @return the copy
     */
    @Override
    public Graph clone() {
        Map<Node, Set<Node>> adjListCopy = new HashMap<>();
        for (Map.Entry<Node, Set<Node>> entry : adjList.entrySet())
        {
            adjListCopy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        Map<Long, Node> nodesCopy = new HashMap<>();
        for (Map.Entry<Long, Node> entry : nodes.entrySet())
        {
            nodesCopy.put(entry.getKey(), entry.getValue());
        }
        return new Graph(nodesCopy, adjListCopy);
    }

    public class Path {

        private final LinkedList<DijkstraNode> dNodes;
        private double pathCost;

        public Path(LinkedList<DijkstraNode> nodes) {
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
            toAppend.dNodes.removeFirst();
            toAppend.dNodes.forEach(dNode -> dNode.distanceFromSource += pathCost);
            dNodes.addAll(toAppend.dNodes);
            pathCost = dNodes.getLast().distanceFromSource;
            return this;
        }

        public Path insert(Path toInsert, int start, int end) {
            // check if parameters are invalid. Passing this check implies that nodes.size() > 0.
            if (dNodes.size() <= Math.max(start, end) || Math.min(start, end) < 0) {
                throw new IllegalArgumentException("Insertion points are out of bounds!");
            }

            //Path front = new Path(new LinkedList<>(dNodes.subList(0, start+1)));
            LinkedList<DijkstraNode> backList = new LinkedList<>(dNodes.subList(end, dNodes.size()));
            double reducedCost = backList.getFirst().distanceFromSource;
            backList.forEach(dNode -> dNode.distanceFromSource -= reducedCost);
            Path back = new Path(backList);

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
