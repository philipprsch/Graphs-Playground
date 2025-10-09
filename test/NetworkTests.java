
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkTests {

    private DirectedGraph<String, Edge<String>> graph;

    @BeforeEach
    void setup() {
        graph = new DirectedGraph<>();
    }
    @Test
    void testAddSingleNode() {
        Node<String> a = graph.addNode("A");
        assertEquals("A", a.getValue());
        assertTrue(graph.getNodes().contains(a));
    }
    @Test
    void testGetAugmentatingNetwork() {

        graph.addEdge("S", "V2", 3.0);
        graph.addEdge("S", "V1", 3.0);
        graph.addEdge("V2", "V4", 3.0);
        graph.addEdge("V2", "V3", 1.0);
        graph.addEdge("V1", "V2", 1.0);
        graph.addEdge("V1", "V3", 1.0);
        graph.addEdge("V3", "T", 3.0);
        graph.addEdge("V4", "V3", 3.0);
        graph.addEdge("V4", "T", 2.0);

        long seed = 12345L;
        Random random = new Random(seed);

        Map<Edge<String>, Double> flow = graph.generateFlow(e -> (double) (random.nextInt(e.getWeight().intValue() + 1)));

        Graph<String, Edge<String>> augmentingNetwork = graph.getAugmentingNetwork(flow);

        String s1 = GraphUtils.toGraphviz(graph, (e) -> "F: " + flow.get(e).toString());
        String s2 = GraphUtils.toGraphviz(augmentingNetwork, (e) -> "");

        System.out.println(s1);
        System.out.println();
        System.out.println(s2);

    }
}