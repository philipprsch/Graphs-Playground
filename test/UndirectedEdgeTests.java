import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class UndirectedEdgeTests {

    private static final Random random = new Random(35903982591067L);

    private UndirectedGraph<String, UndirectedEdge<String>> graph;

    @BeforeEach
    void setup() {
        graph = new UndirectedGraph<>();
    }
    @Test
    void testAcyclicShortestPathExample() {
        UndirectedEdge<String> edge1 =  graph.addEdge("S", "T");
        //graph.addEdge("S", "V2");
        System.out.println(graph.getReachableNodes(graph.getNode("T")));
        System.out.println(graph.getReachableNodes(graph.getNode("S")));

        graph.removeEdge((UndirectedEdge<String>) edge1.reversed());

        System.out.println(graph.getReachableNodes(graph.getNode("T")));
        System.out.println(graph.getReachableNodes(graph.getNode("S")));
    }
}
