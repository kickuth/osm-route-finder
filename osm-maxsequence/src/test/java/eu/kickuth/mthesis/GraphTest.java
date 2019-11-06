package eu.kickuth.mthesis;

import eu.kickuth.mthesis.graph.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import eu.kickuth.mthesis.graph.Graph.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphTest {

    private static final Logger logger = LogManager.getLogger(GraphTest.class);

    private final Graph g = new Graph(new double[]{90, -90, -180, 180});
    private Dijkstra dijkstra;

    private final double EPSILON = 10e-6;

    @BeforeEach
    void setUp() {
        Node n0 = new Node(0, 30, 30, "Origin");
        Node n1 = new Node(1, 30, 31, "01");
        Node n2 = new Node(2, 31, 30, "10");
        Node n3 = new Node(3, 31, 31, "11");
        Node n4 = new Node(4, 30, 32, "02");
        Node n5 = new Node(5, 32, 32, "22");
        Node n6 = new Node(6, 30.5, 30.5, "01");
        g.addNode(n0);
        g.addNode(n1);
        g.addNode(n2);
        g.addNode(n3);
        g.addNode(n4);
        g.addNode(n5);
        g.addNode(n6);

        g.addEdge(n0, n1);
        g.addEdge(n0, n2);
        g.addEdge(n1, n0);
        g.addEdge(n1, n4);
        g.addEdge(n1, n6);
        g.addEdge(n2, n0);
        g.addEdge(n3, n2);
        g.addEdge(n3, n5);
        g.addEdge(n4, n1);
        g.addEdge(n4, n5);
        g.addEdge(n5, n4);
        g.addEdge(n6, n3);

        dijkstra = new Dijkstra(g);
    }

    @Test
    void simpleAppendTest() {
        logger.trace("Running simplePathTest");

        Path p = getShortestPath(3, 6); // 3, 2, 0, 1, 6
        Path q = getShortestPath(0, 3); // 0, 1, 6, 3

        assertThrows(IllegalArgumentException.class, () -> p.append(p)); // TODO move to own test for edge cases

        LinkedList<Node> qNodesTest = new LinkedList<>();
        qNodesTest.add(g.getNode(0));
        qNodesTest.add(g.getNode(1));
        qNodesTest.add(g.getNode(6));
        qNodesTest.add(g.getNode(3));

        assertEquals(qNodesTest, q.getNodes());

        double addedCost = q.getPathCost() + p.getPathCost();
        q.append(p);
        assertEquals(addedCost, q.getPathCost(), EPSILON);

        LinkedList<Node> pNodes = p.getNodes();
        pNodes.removeFirst();
        qNodesTest.addAll(pNodes);

        assertEquals(qNodesTest, q.getNodes());

        logger.trace("Testpath after concatination: {}", q.toString());
    }

    @Test
    void simpleInsertTest() {
        Path p = getShortestPath(5, 3);
        Path q = getShortestPath(4, 6);
        double costBeforeInsert = p.getPathCost();
        p.insert(q, 1, 3);
        assertEquals(costBeforeInsert, p.getPathCost(), EPSILON);

        Path a = getShortestPath(1, 5);
        Path b = getShortestPath(5, 1);
        a.append(b);
        Path c = getShortestPath(1, 2);
        Path d = getShortestPath(2, 1);
        c.append(d);
        a.insert(c, 5, 5);

        logger.trace("Testpath after insert: {}", a);
    }

    Path getShortestPath(int sourceId, int targetId) {
        return dijkstra.shortestPath(g.getNode(sourceId), g.getNode(targetId));
    }
}
