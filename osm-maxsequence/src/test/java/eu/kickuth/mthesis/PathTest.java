package eu.kickuth.mthesis;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.graph.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PathTest {


    private final Graph graph = new Graph();

    @BeforeEach
    void setUp() {
        Node n0 = new Node(0, 0, 0, "Origin");
        Node n1 = new Node(1, 0, 1, "01");
        Node n2 = new Node(2, 1, 0, "10");
        Node n3 = new Node(3, 1, 1, "11");
        Node n4 = new Node(4, 0, 2, "02");
        // Node n5 = new Node(5, 2, 0, "20");
        Node n6 = new Node(6, 2, 2, "22");
        Node n7 = new Node(7, 0.5, 0.5, "01");
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
        graph.addEdge(n2, n3);
        graph.addEdge(n3, n2);
        graph.addEdge(n3, n6);
        graph.addEdge(n4, n1);
        graph.addEdge(n4, n6);
        graph.addEdge(n6, n4);
        graph.addEdge(n7, n3);
    }

    @Test
    void insertTest() {
        Path p = new Path(graph);
        assertThrows(IllegalArgumentException.class, () -> {
            p.insert(p, 0, 0);
        });

    }
}
