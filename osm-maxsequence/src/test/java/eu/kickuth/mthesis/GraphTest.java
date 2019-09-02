package eu.kickuth.mthesis;

import eu.kickuth.mthesis.graph.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GraphTest {


    private final Graph graph = new Graph();
    private Dijkstra dijkstra;

    @BeforeEach
    void setUp() {
        Node n0 = new Node(0, 30, 30, "Origin");
        Node n1 = new Node(1, 30, 31, "01");
        Node n2 = new Node(2, 31, 30, "10");
        Node n3 = new Node(3, 31, 31, "11");
        Node n4 = new Node(4, 30, 32, "02");
        // Node n5 = new Node(5, 32, 30, "20");
        Node n6 = new Node(6, 32, 32, "22");
        Node n7 = new Node(7, 30.5, 30.5, "01");
        graph.addNode(n0);
        graph.addNode(n1);
        graph.addNode(n2);
        graph.addNode(n3);
        graph.addNode(n4);
        // graph.addNode(n5);
        graph.addNode(n6);
        graph.addNode(n7);

        graph.addEdge(n0, n1);
        graph.addEdge(n0, n2);
        graph.addEdge(n1, n0);
        graph.addEdge(n1, n4);
        graph.addEdge(n1, n7);
        graph.addEdge(n2, n0);
        graph.addEdge(n3, n2);
        graph.addEdge(n3, n6);
        graph.addEdge(n4, n1);
        graph.addEdge(n4, n6);
        graph.addEdge(n6, n4);
        graph.addEdge(n7, n3);

        dijkstra = new Dijkstra(graph);
    }

    @Test
    void simplePathTest() {
        Path p = new Path(graph);
        assertThrows(IllegalArgumentException.class, () -> {
            p.insert(p, 0, 0);
        });

        // TODO redo test after refactor
        Path q = new Path((LinkedList<DijkstraNode>)dijkstra.shortestPath(graph.getNode(0), graph.getNode(3)), graph);
        LinkedList<Node> qNodesTest = new LinkedList<>();
        qNodesTest.add(graph.getNode(0));
        qNodesTest.add(graph.getNode(1));
        qNodesTest.add(graph.getNode(7));
        qNodesTest.add(graph.getNode(3));
        assertEquals(q.getNodes(), qNodesTest);
        System.out.println(q.toString());
        //q.insert()
        // TODO
    }
}
