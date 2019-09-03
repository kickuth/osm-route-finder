package eu.kickuth.mthesis.graph;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class Path {

    private final LinkedList<DijkstraNode> dNodes;
    private final Graph graph;
    private double pathCost;

    public Path(LinkedList<DijkstraNode> nodes, Graph g) {
        graph = g;
        this.dNodes = nodes;
        pathCost = (nodes.isEmpty() ? 0 : nodes.getLast().distanceFromSource);
    }

    public Path(Graph g) {
        graph = g;
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
        // check if parameters are invalid. Passing this check implies that nodes.size() > 0.
        if (dNodes.size() <= Math.max(start, end) || Math.min(start, end) < 0) {
            throw new IllegalArgumentException("Insertion points are out of bounds!");
        }

        Path front = new Path(new LinkedList<>(dNodes.subList(0, start+1)), graph);
        LinkedList<DijkstraNode> backList = new LinkedList<>(dNodes.subList(end, dNodes.size()));
        double reducedCost = backList.getFirst().distanceFromSource;
        backList.forEach(dNode -> dNode.distanceFromSource -= reducedCost);
        Path back = new Path(backList, graph);

        return front.append(toInsert).append(back);
    }

    public LinkedList<Node> getNodes() {
        return dNodes.stream().map(dNode -> dNode.node).collect(Collectors.toCollection(LinkedList::new));
    }

    public double getPathCost() {
        return pathCost;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Path: ");
        if (dNodes.size() < 15) {
            for (DijkstraNode dNode : dNodes) {
                s.append(dNode.node.getId()).append(", ");
            }
        } else {
            s.append("Number of Nodes on path: ").append(dNodes.size());
        }
        s.append("Total length: ").append(pathCost);
        return s.toString();
    }
}
