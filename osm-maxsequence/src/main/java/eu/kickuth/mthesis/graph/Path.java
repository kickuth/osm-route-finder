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
        if (dNodes.isEmpty()) {
            dNodes.addAll(toAppend.dNodes);
            pathCost = toAppend.pathCost;
        } else if (toAppend.dNodes.isEmpty() || !dNodes.getLast().node.equals(toAppend.dNodes.getFirst().node)) {
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
        // check if parameters are invalid. Passing this check implies that nodes.size() > 0.
        if (dNodes.size() <= Math.max(start, end) || Math.min(start, end) < 0) {
            throw new IllegalArgumentException("Insertion points are out of bounds!");
        }
        // check trivial case
        if (toInsert.dNodes.size() <= 1) {
            // TODO reduce (or extend) this.nodes according to start/end points
            return this;
        }

        if (start == end) {
            return insertAt(toInsert, start);
        }
        if (start < end) {
            double costAtInsert = dNodes.get(start).distanceFromSource;
            double replacedCost = dNodes.get(end).distanceFromSource - costAtInsert;
            double costOfInsert = toInsert.pathCost;
            // prepare inserted path
            Node startNode = toInsert.dNodes.pop().node; // remove first node
            if (!startNode.equals(dNodes.get(start).node)) {
                throw new IllegalArgumentException("Paths can not be joined!");
            }
            toInsert.dNodes.forEach(dNode -> dNode.distanceFromSource += costAtInsert);
            toInsert.pathCost += costAtInsert;

            // adjust nodes following after inserted path TODO end+1?
            dNodes.listIterator(end).forEachRemaining(dNode -> dNode.distanceFromSource += (costOfInsert - replacedCost));

            //remove to be replaced nodes
            ListIterator<DijkstraNode> iter = dNodes.listIterator(start);
            for (int i = start; i < end; i++) {
                iter.remove();
                // TODO iter.next(); required?
            }

            // insert path
            dNodes.addAll(start, toInsert.dNodes);
            pathCost = dNodes.getLast().distanceFromSource;

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
