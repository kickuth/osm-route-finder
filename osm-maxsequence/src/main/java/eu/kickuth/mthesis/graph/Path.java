package eu.kickuth.mthesis.graph;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class Path {

    private final LinkedList<DijkstraNode> nodes;
    private final Graph graph;
    private double pathCost;

    public Path(LinkedList<DijkstraNode> nodes, Graph g) {
        graph = g;
        this.nodes = nodes;
        pathCost = (nodes.isEmpty() ? 0 : nodes.getLast().distanceFromSource);
    }

    public Path(Graph g) {
        graph = g;
        nodes = new LinkedList<>();
        pathCost = 0;
    }

    public void append(Path toAppend) {
        if (nodes.isEmpty()) {
            nodes.addAll(toAppend.nodes);  // TODO toAppend.nodes accessible?
            pathCost = toAppend.pathCost;
        } else if (!toAppend.nodes.isEmpty()){
            if (graph.adjList.get(nodes.getLast().node).contains(toAppend.nodes.getFirst().node)) {
                toAppend.nodes.forEach(dNode -> dNode.distanceFromSource += pathCost);
                nodes.removeLast();
                nodes.addAll(toAppend.nodes);
                pathCost = nodes.getLast().distanceFromSource;
            }
        }
    }

    public LinkedList<Node> getNodes() {
        return nodes.stream().map(dNode -> dNode.node).collect(Collectors.toCollection(LinkedList::new));
    }
}
