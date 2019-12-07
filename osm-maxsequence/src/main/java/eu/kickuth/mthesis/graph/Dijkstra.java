package eu.kickuth.mthesis.graph;

import java.util.*;

import eu.kickuth.mthesis.graph.Graph.Path;

public class Dijkstra {

    private static final Map<Graph, Dijkstra> instances = new HashMap<>();

    private final Graph graph;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final DijkstraNode[] pqueueNodes;
    private List<Integer> updatedPqueueNodes;  // keep track of which nodes need resetting after dijkstra run
    private int[] parentMap;  // keep track of a nodes Parent for a dijkstra run
    private boolean[] isTarget;  // mark dijkstra targets in array to quickly check if a node is a target
    private Set<Node> pathCandidates = new HashSet<>();

    private double shortestPathCost;  // target distance
    private double maxDistance;
    private Graph.Path stPath;  // shortest st path

    private double[] updateForwardCosts;

    /**
     * Get a Dijkstra instance for given graph. Will only create a single instance per graph.
     * @param graph graph for the Dijkstra instance
     * @return unique Dijkstra object for given graph
     */
    public static Dijkstra getInstance(Graph graph) {
        Dijkstra instance = instances.get(graph);
        return (instance == null ? new Dijkstra(graph) : instance);
    }

    /**
     * Create a new Dijkstra instance and add it to the instance map.
     * @param g The graph belonging to this instance
     */
    private Dijkstra(Graph g) {
        graph = g;
        int nodeCount = graph.nodes.size();
        pqueue = new PriorityQueue<>(nodeCount);
        pqueueNodes = graph.nodes.stream().map(node -> new DijkstraNode(node, Double.POSITIVE_INFINITY)).toArray(DijkstraNode[]::new);
        updatedPqueueNodes = new ArrayList<>(10_000);
        parentMap = new int[nodeCount];
        Arrays.fill(parentMap, -1);
        isTarget = new boolean[nodeCount];
        instances.put(graph, this);
        stPath = graph.new Path();
        updateForwardCosts = new double[nodeCount];
        Arrays.fill(updateForwardCosts, Double.POSITIVE_INFINITY);
    }


    public void update(final Node source, final Node target, final double maxDistanceFactor) {
        clean();

        // initialise queue
        pqueueNodes[source.id].distanceFromSource = 0;
        updatedPqueueNodes.add(source.id);
        pqueue.add(pqueueNodes[source.id]);

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();

            // check whether an updated node has already been processed
            if (currentMin.wasProcessed) {
                continue;
            } else {
                currentMin.wasProcessed = true;
                updateForwardCosts[currentMin.node.id] = currentMin.distanceFromSource;
            }

            // are we passing the target? --> set shortest path and s-t distance
            if (currentMin.node.id == target.id) {
                shortestPathCost = currentMin.distanceFromSource;
                maxDistance = shortestPathCost * maxDistanceFactor;
                retrieveShortestPath(target.id);
            }
            // get and potentially update all neighbours
            for (Edge toNeighbour : graph.adjList.get(currentMin.node.id)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;

                // ignore neighbours that are too far
                if (alternativeDistance > maxDistance) {
                    continue;
                }

                Node neighbour = toNeighbour.dest;

                // check that b-line to target is short enough
                if (maxDistance < Double.POSITIVE_INFINITY &&
                        alternativeDistance + neighbour.getDistance(target) > maxDistance) {
                    continue;
                }

                // update queue, if the new path is shorter than the previous shortest
                if (checkNewDistance(pqueueNodes[neighbour.id], alternativeDistance)) {
                    parentMap[neighbour.id] = currentMin.node.id;
                }
            }
        }

        computePathCandidates(target, updateForwardCosts);
    }

    private void retrieveShortestPath(int targetId) {
        LinkedList<DijkstraNode> results = new LinkedList<>();
        while (targetId != -1) {
            DijkstraNode backtrack = pqueueNodes[targetId];
            results.addFirst(new DijkstraNode(backtrack.node, backtrack.distanceFromSource));
            targetId = parentMap[targetId];
        }
        stPath = graph.new Path(results);
    }

    public double getShortestPathCost() {
        return shortestPathCost;
    }

    // TODO getter for maxDistance?!

    public Graph.Path getShortestPath() {
        return stPath;
    }

    public Set<Node> getPathCandidates() {
        return pathCandidates;
    }

    /**
     * Compute a single source shortest path to the closest target node
     * @param source the node to start with
     * @param targets set of target nodes
     * @return path from source to closest target
     */
    public Path shortestPath(final Node source, final Collection<Node> targets) {
        ArrayList<Node> sources = new ArrayList<>(1);
        sources.add(source);
        return shortestPath(sources, targets);

    }

    /**
     * Compute the shortest s-t-path
     * @param source the source node
     * @param target the target node
     * @return Path of nodes from source to target, empty Path if no path exists
     */
    public Path shortestPath(final Node source, final Node target) {
        List<Node> sources = new ArrayList<>(1);
        List<Node> targets = new ArrayList<>(1);
        sources.add(source);
        targets.add(target);
        return shortestPath(sources, targets);
    }

    /**
     * Compute the shortest path from any source to any target node
     * @param sources the set of source nodes
     * @param targets the set of target nodes
     * @return Path of nodes for the shortest s-t-path, empty Path if no path exists
     */
    public Path shortestPath(final Collection<Node> sources, final Collection<Node> targets) {
        clean();
        // initialise
        for (Node source : sources) {
            pqueueNodes[source.id].distanceFromSource = 0;
            updatedPqueueNodes.add(source.id);
            pqueue.add(pqueueNodes[source.id]);
            parentMap[source.id] = -1;
        }
        targets.forEach(node -> isTarget[node.id] = true);

        // the first reached node in targets
        int backtrackId = -1;

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();

            // check whether an updated node has already been processed
            if (currentMin.wasProcessed) {
                continue;
            } else {
                currentMin.wasProcessed = true;
            }

            // are we at the target yet?
            if (isTarget[currentMin.node.id]) {
                backtrackId = currentMin.node.id;
                break;
            }
            // get and potentially update all neighbours
            for (Edge toNeighbour : graph.adjList.get(currentMin.node.id)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;
                // update queue, if the new path is shorter than the previous shortest
                Node neighbour = toNeighbour.dest;
                if (checkNewDistance(pqueueNodes[neighbour.id], alternativeDistance)) {
                    parentMap[neighbour.id] = currentMin.node.id;
                }
            }
        }

        // reset target array
        targets.forEach(node -> isTarget[node.id] = false);

        // reconstruct the path from the target
        retrieveShortestPath(backtrackId);

        return stPath;
    }

    private void computePathCandidates(Node target, double[] forwardRunCosts) {
        // TODO logs (or rewrite first)
        clean();

        pqueueNodes[target.id].distanceFromSource = 0;
        updatedPqueueNodes.add(target.id);
        pqueue.add(pqueueNodes[target.id]);

        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();

            // check whether an updated node has already been processed
            if (currentMin.wasProcessed || maxDistance < currentMin.distanceFromSource + forwardRunCosts[currentMin.node.id]) {
                continue;
            } else {
                currentMin.wasProcessed = true;
                pathCandidates.add(currentMin.node);
            }

            // get and potentially update all neighbours
            for (Edge toNeighbour : graph.adjListRev.get(currentMin.node.id)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;
                // update queue, if the new path is shorter than the previous shortest
                checkNewDistance(pqueueNodes[toNeighbour.source.id], alternativeDistance);
            }
        }
        Arrays.fill(forwardRunCosts, Double.POSITIVE_INFINITY);
    }

    /**
     * reset instance arrays for new dijkstra computation
     */
    private void clean() {
        updatedPqueueNodes.parallelStream().forEach(index -> {
            DijkstraNode dNode = pqueueNodes[index];
            dNode.distanceFromSource = Double.POSITIVE_INFINITY;
            dNode.wasProcessed = false;
        });
        maxDistance = Double.POSITIVE_INFINITY;
        updatedPqueueNodes.clear();
        pqueue.clear();
        pathCandidates.clear();
    }

    /**
     * Check if the new distance to a node is smaller than the previously known distance. If so, update priority queue
     * and instance lists.
     * @param dNode node in question
     * @param altDist new distance to node
     * @return true if the new distance was smaller, else false.
     */
    private boolean checkNewDistance(DijkstraNode dNode, double altDist) {
        if (altDist < dNode.distanceFromSource) {
            // only add to updated list, if it was not already updated
            if (dNode.distanceFromSource == Double.POSITIVE_INFINITY) {
                updatedPqueueNodes.add(dNode.node.id);
            }
            dNode = new DijkstraNode(dNode.node, altDist);
            pqueueNodes[dNode.node.id] = dNode;
            pqueue.add(dNode);
            return true;
        }
        return false;
    }
}