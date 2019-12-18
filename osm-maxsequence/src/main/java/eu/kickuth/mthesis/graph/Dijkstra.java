package eu.kickuth.mthesis.graph;

import java.util.*;

import eu.kickuth.mthesis.graph.Graph.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dijkstra {

    private static final Logger logger = LogManager.getLogger(Dijkstra.class);
    private static final Map<Graph, Dijkstra> instances = new HashMap<>();

    private final Graph graph;
    private final PriorityQueue<DijkstraNode> pqueue;
    private final DijkstraNode[] pqueueNodes;
    private List<Integer> updatedPqueueNodes;  // keep track of which nodes need resetting after dijkstra run
    private int[] parentMap;  // keep track of a nodes Parent for a dijkstra run
    private boolean[] settledNodes;  // keep track of already extracted nodes
    private boolean[] isTarget;  // mark dijkstra targets in array to quickly check if a node is a target
    private Set<Node> pathCandidates = new HashSet<>();

    private double shortestPathCost;  // target distance
    private double maxDistance = Double.POSITIVE_INFINITY;
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
        settledNodes = new boolean[nodeCount];
        isTarget = new boolean[nodeCount];
        instances.put(graph, this);
        stPath = graph.new Path();
        updateForwardCosts = new double[nodeCount];
    }


    public void update(final Node source, final Node target, final double maxDistanceFactor) {
        clean();
        Arrays.fill(updateForwardCosts, Double.POSITIVE_INFINITY);

        // initialise queue
        pqueueNodes[source.id].distanceFromSource = 0;
        updatedPqueueNodes.add(source.id);
        pqueue.add(pqueueNodes[source.id]);

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();
            int currentId = currentMin.node.id;

            // check whether an updated node has already been processed
            if (settledNodes[currentId]) {
                continue;
            } else {
                settledNodes[currentId] = true;
                updateForwardCosts[currentId] = currentMin.distanceFromSource;
            }

            // are we passing the target? --> set shortest path and s-t distance
            if (currentId == target.id) {
                shortestPathCost = currentMin.distanceFromSource;
                maxDistance = shortestPathCost * maxDistanceFactor;
                retrieveShortestPath(target.id);
                logger.debug("found target. Shortest path dist is {}.", shortestPathCost);
            }
            // get and potentially update all neighbours
            for (Edge toNeighbour : graph.adjList.get(currentId)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;

                // ignore neighbours that are too far
                if (alternativeDistance > maxDistance) {
                    continue;
                }

                Node neighbour = toNeighbour.dest;

                // check that b-line to target is short enough (starting after we know the max distance)
                if (maxDistance < Double.POSITIVE_INFINITY &&
                        alternativeDistance + neighbour.getDistance(target) > maxDistance) {
                    continue;
                }

                // update queue, if the new path is shorter than the previous shortest
                if (checkNewDistance(pqueueNodes[neighbour.id], alternativeDistance)) {
                    parentMap[neighbour.id] = currentId;
                }
            }
        }
        logger.trace("Finished forward pass of update.");

        computePathCandidates(target);
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

    public Graph.Path getShortestPath() {
        return stPath.copy();
    }

    public Set<Node> getPathCandidates() {
        return pathCandidates;
    }

    /**
     * Compute the shortest s-t-path
     * @param source the node to start with
     * @param targets set of target nodes
     * @param reverse reverse computing direction (t to s)?
     * @return Shortest path from a source to a target, empty Path if no path exists
     */
    public Path shortestPath(final Node source, final Collection<Node> targets, boolean reverse) {
        return shortestPath(Collections.singletonList(source), targets, reverse);
    }
    public Path shortestPath(final Node source, final Node target, final boolean reverse) {
        return shortestPath(Collections.singletonList(source), Collections.singletonList(target), reverse);
    }
    public Path shortestPath(final Collection<Node> sources, final Node target, final boolean reverse) {
        return shortestPath(sources, Collections.singletonList(target), reverse);
    }

    /**
     * Compute the shortest path from any source to any target node
     * @param sources the set of source nodes
     * @param targets the set of target nodes
     * @param reverse reverse computing direction (t to s)?
     * @return Path of nodes for the shortest s-t-path, empty Path if no path exists
     */
    public Path shortestPath(final Collection<Node> sources, final Collection<Node> targets, final boolean reverse) {
        clean();

        if (reverse) {
            return shortestPathRev(sources, targets);
        }
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
            int currentId = currentMin.node.id;

            // check whether an updated node has already been processed
            if (settledNodes[currentId]) {
                continue;
            } else {
                settledNodes[currentId] = true;
            }

            // are we at the target yet?
            if (isTarget[currentId]) {
                backtrackId = currentId;
                break;
            }
            // get and potentially update all neighbours
            for (Edge toNeighbour : graph.adjList.get(currentId)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;
                // update queue, if the new path is shorter than the previous shortest
                Node neighbour = toNeighbour.dest;
                if (checkNewDistance(pqueueNodes[neighbour.id], alternativeDistance)) {
                    parentMap[neighbour.id] = currentId;
                }
            }
        }

        // reset target array
        targets.forEach(node -> isTarget[node.id] = false);

        // reconstruct the path from the target
        retrieveShortestPath(backtrackId);

        return stPath;
    }

    private Path shortestPathRev(Collection<Node> sources, Collection<Node> targets) {
        // initialise
        for (Node target : targets) {
            pqueueNodes[target.id].distanceFromSource = 0;
            updatedPqueueNodes.add(target.id);
            pqueue.add(pqueueNodes[target.id]);
            parentMap[target.id] = -1;
        }
        sources.forEach(node -> isTarget[node.id] = true);

        // the first reached source node
        int backtrackId = -1;

        // main loop
        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();
            int currentId = currentMin.node.id;

            // check whether an updated node has already been processed
            if (settledNodes[currentId]) {
                continue;
            } else {
                settledNodes[currentId] = true;
            }

            // are we at a source yet?
            if (isTarget[currentId]) {
                backtrackId = currentId;
                break;
            }
            // get and potentially update all (back edge) neighbours
            for (Edge toNeighbour : graph.adjListRev.get(currentId)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;
                // update queue, if the new path is shorter than the previous shortest
                Node neighbour = toNeighbour.source;
                if (checkNewDistance(pqueueNodes[neighbour.id], alternativeDistance)) {
                    parentMap[neighbour.id] = currentId;
                }
            }
        }

        // reset target array
        sources.forEach(node -> isTarget[node.id] = false);

        // reconstruct the path from the target
        LinkedList<DijkstraNode> results = new LinkedList<>();
        DijkstraNode backtrack;
        double pathCost;
        if (backtrackId != -1) {
            pathCost = pqueueNodes[backtrackId].distanceFromSource;
            do {
                backtrack = pqueueNodes[backtrackId];
                results.addLast(new DijkstraNode(backtrack.node, pathCost - backtrack.distanceFromSource));
                backtrackId = parentMap[backtrackId];
            } while (backtrackId != -1);
        }
        stPath = graph.new Path(results);

        return stPath;
    }

    private void computePathCandidates(Node target) {
        // TODO logs/comments (or rewrite first)
        // TODO pathCandidates as bool array? --> shortest path only look at pathCandidates
        clean();

        pqueueNodes[target.id].distanceFromSource = 0;
        updatedPqueueNodes.add(target.id);
        pqueue.add(pqueueNodes[target.id]);

        while (!pqueue.isEmpty()) {
            DijkstraNode currentMin = pqueue.poll();
            int currentId = currentMin.node.id;

            // check whether an updated node has already been processed
            if (settledNodes[currentId] || maxDistance < currentMin.distanceFromSource + updateForwardCosts[currentId]) {
                continue;
            } else {
                settledNodes[currentId] = true;
                pathCandidates.add(currentMin.node);
            }

            // get and potentially update all neighbours
            for (Edge toNeighbour : graph.adjListRev.get(currentId)) {
                double alternativeDistance = currentMin.distanceFromSource + toNeighbour.cost;
                // update queue, if the new path is shorter than the previous shortest
                checkNewDistance(pqueueNodes[toNeighbour.source.id], alternativeDistance);
            }
        }
        logger.trace("Number of reachable nodes: {}", pathCandidates.size());
        maxDistance = Double.POSITIVE_INFINITY;
        logger.trace("Dijkstra update complete.");
    }

    /**
     * reset instance arrays for new dijkstra computation
     */
    private void clean() {
        updatedPqueueNodes.parallelStream().forEach(index -> {
            DijkstraNode dNode = pqueueNodes[index];
            dNode.distanceFromSource = Double.POSITIVE_INFINITY;
            settledNodes[index] = false;
            parentMap[index] = -1;
        });
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

    public double[] getUpdateForwardCosts() {
        return updateForwardCosts;
    }
}
