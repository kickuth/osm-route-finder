package eu.kickuth.mthesis.graph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
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

    public Path append(Path toAppend) {
        if (nodes.isEmpty()) {
            nodes.addAll(toAppend.nodes);
            pathCost = toAppend.pathCost;
        } else if (!toAppend.nodes.isEmpty()) {
            if (graph.adjList.get(nodes.getLast().node).contains(toAppend.nodes.getFirst().node)) {
                toAppend.nodes.forEach(dNode -> dNode.distanceFromSource += pathCost);
                nodes.removeLast();
                nodes.addAll(toAppend.nodes);
                pathCost = nodes.getLast().distanceFromSource;
            }
        }
        return this;
    }

    public Path insert(Path toInsert, int start, int end) {
        // check if parameters are invalid. Passing this check implies that nodes.size() > 0.
        if (nodes.size() <= Math.max(start, end) || Math.min(start, end) < 0) {
            throw new IllegalArgumentException("Insertion points are out of bounds!");
        }
        // check trivial case
        if (toInsert.nodes.size() <= 1) {
            // TODO reduce (or extend) this.nodes according to start/end points
            return this;
        }

        if (start == end) {
            return insertAt(toInsert, start);
        }
        if (start < end) {
            double costAtInsert = nodes.get(start).distanceFromSource;
            double replacedCost = nodes.get(end).distanceFromSource - costAtInsert;
            double costOfInsert = toInsert.pathCost;
            // prepare inserted path
            Node startNode = toInsert.nodes.pop().node; // remove first node
            if (!startNode.equals(nodes.get(start).node)) {
                throw new IllegalArgumentException("Paths can not be joined!");
            }
            toInsert.nodes.forEach(dNode -> dNode.distanceFromSource += costAtInsert);
            toInsert.pathCost += costAtInsert;

            // adjust nodes following after inserted path TODO end+1?
            nodes.listIterator(end).forEachRemaining(dNode -> dNode.distanceFromSource += (costOfInsert - replacedCost));

            //remove to be replaced nodes
            ListIterator<DijkstraNode> iter = nodes.listIterator(start);
            for (int i = start; i < end; i++) {
                iter.remove();
                // TODO iter.next(); required?
            }

            // insert path
            nodes.addAll(start, toInsert.nodes);
            pathCost = nodes.getLast().distanceFromSource;

            return this;
        }

        // start > end
        // TODO
        return null;
    }

    private Path insertAt(Path toInsert, int start) {
        //TODO
        return null;
    }


    public LinkedList<Node> getNodes() {
        return nodes.stream().map(dNode -> dNode.node).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Path: ");
        if (nodes.size() < 15) {
            for (DijkstraNode dNode : nodes) {
                s.append(dNode.node.getId()).append(", ");
            }
        } else {
            s.append("Number of Nodes on path: ").append(nodes.size());
        }
        s.append("Total length: ").append(pathCost);
        return s.toString();
    }
}
