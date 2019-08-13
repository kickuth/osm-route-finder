package eu.kickuth.mthesis;

import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class Graph {

    // TODO make graph immutable? --> (inner) GraphBuilder class

    public Map<Node, List<Node>> adjList;

    public Graph(Map<Node, List<Node>> adjList) {
        this.adjList = adjList;
    }

    public Graph() {
        adjList = new HashMap<>();
    }

    public Graph(int nodeCountEstimate) {
        adjList = new HashMap<>(nodeCountEstimate + nodeCountEstimate / 3);
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
            adjList.put(toAdd, new LinkedList<>());
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
        List<Node> sourceNeighbours = adjList.get(source);
        if (sourceNeighbours.contains(dest)) {
            return false;
        }
        // add edge
        sourceNeighbours.add(dest);
        return true;
    }

    public Graph createSubgraph(Set<Node> nodeSubset) {
        Graph subGraph = new Graph(nodeSubset.size());
        // populate subgraph with nodes
        for (Node node : nodeSubset) {
            subGraph.addNode(node);
        }

        // add edges present in both graphs
        for (Node node : nodeSubset) {
            List<Node> neighbours = adjList.get(node);
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
        Map<Node, List<Node>> copy = new HashMap<>();
        for (Map.Entry<Node, List<Node>> entry : adjList.entrySet())
        {
            copy.put(entry.getKey(), new LinkedList<>(entry.getValue()));
        }
        return new Graph(copy);
    }

}
