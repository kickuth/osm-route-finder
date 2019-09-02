package eu.kickuth.mthesis.graph;

import java.util.*;

public class Graph {

    public Map<Node, Set<Node>> adjList;
    private Map<Long, Node> nodes;

    public Graph(Map<Long, Node> nodes, Map<Node, Set<Node>> adjList) {
        this.nodes = nodes;
        this.adjList = adjList;
    }

    public Graph() {
        adjList = new HashMap<>();
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

}
