
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkTests {

    private Network<String, Edge<String>> graph;

    @BeforeEach
    void setup() {
        graph = new Network<>("S", "T");
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

        Graph<String, AugmentingEdge<String>> augmentingNetwork = graph.getAugmentingNetwork(flow);

        String s1 = GraphUtils.toGraphviz(graph, (e) -> "F: " + flow.get(e).toString());
        String s2 = GraphUtils.toGraphviz(augmentingNetwork, (e) -> "");

        System.out.println(s1);
        System.out.println();
        System.out.println(s2);

    }
    @Test
    void testMaximizeFlow() {
        graph.addEdge("S", "V2", 3.0);
        graph.addEdge("S", "V1", 3.0);
        graph.addEdge("V2", "V4", 3.0);
        graph.addEdge("V2", "V3", 1.0);
        graph.addEdge("V1", "V2", 1.0);
        graph.addEdge("V1", "V3", 1.0);
        graph.addEdge("V3", "T", 3.0);
        graph.addEdge("V4", "V3", 3.0);
        graph.addEdge("V4", "T", 2.0);

        Map<Edge<String>, Double> flow = graph.maximizeFlow();

        Graph<String, AugmentingEdge<String>> augmentingNetwork = graph.getAugmentingNetwork(flow);

        String s1 = GraphUtils.toGraphviz(graph, (e) -> "F: " + flow.get(e).toString());
        String s2 = GraphUtils.toGraphviz(augmentingNetwork, (e) -> "");

        System.out.println(s1);
        System.out.println();
        System.out.println(s2);

    }

    //Test the theory, that after termination of Ford Fulkerson (-> we have maximized flow)
    //the set of edges outgoing form the nodes reachable from s, is the same as the set of
    //edges that were used by augmentAlongPath calls to calculate gamma ("were reversed" in the Augmentationsnetzwerk)
    @Test
    void testMinCutTheory() {
        Network<String, Edge<String>> network = GraphUtils.generateRandomNetwork(
                20,          // number of nodes
                0.25,        // probability of edge
                42L,         // seed for reproducibility
                0.8,         // bias (0.8 = 80% edges forward)
                i -> (i == 0 ? "S" : (i == 19 ? "T" : "V" + i))
        );
        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(List.of(GraphUtils.toGraphviz(network)), List.of("Random network")));
        Set<Edge<String>> augmentingMinEdges = new HashSet<>();
        Map<Edge<String>, Double> maxFLow = network.maximizeFlow(network.getZeroFlow(), augmentingMinEdges);


        Network<String, AugmentingEdge<String>> augmentingNetwork = network.getAugmentingNetwork(maxFLow);
        Set<Node<String>> reachableNodes = augmentingNetwork.getReachableNodes(augmentingNetwork.getS());

        Set<Edge<String>> reachableEdges = reachableNodes.stream().flatMap(n -> {
            return n.getOutgoingEdges().stream().map(e -> {
                return ((AugmentingEdge<String>) e).getOriginalEdge();
            });
        }).collect(Collectors.toSet());
        assertEquals(reachableEdges, augmentingMinEdges);
    }

}