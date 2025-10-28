
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

        AugmentingNetwork<String> augmentingNetwork = AugmentingNetwork.of(graph, flow);

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

        AugmentingNetwork<String> augmentingNetwork = AugmentingNetwork.of(graph, flow);

        String s1 = GraphUtils.toGraphviz(graph, (e) -> "F: " + flow.get(e).toString());
        String s2 = GraphUtils.toGraphviz(augmentingNetwork, (e) -> "");

        System.out.println(s1);
        System.out.println();
        System.out.println(s2);

    }

    //Test the theory, that after termination of Ford Fulkerson (-> we have maximized flow)
    //the set of edges outgoing form the nodes reachable from s, is the same as the set of
    //edges that were used by augmentAlongPath calls to calculate gamma ("were reversed" in the Augmentationsnetzwerk)

    //Conclusion: Theory is false, even "worse" Augmenting paths used are not sufficient to find min-cut-edges
    @Test
    void testMinCutTheory() {
        int totalNodes = 10; // number of nodes
        Random random = new Random(35903982591067L);
        for (int d = 0; d < 100; d++) {
            Network<String, Edge<String>> network = GraphUtils.generateRandomNetwork(
                    totalNodes,          // number of nodes
                    0.25,        // probability of edge
                    random,         // seed for reproducibility
                    0.8,         // bias (0.8 = 80% edges forward)
                    (i, nodeCount) -> (i == 0 ? "S" : (i == nodeCount -1 ? "T" : "V" + i))
            );
            String randomNetworkGraphviz = GraphUtils.combineIntoClusteredGraph(List.of(GraphUtils.toGraphviz(network)), List.of("Random network"));
            Set<Edge<String>> augmentingMinEdges = new HashSet<>();
            //augmentingMinEdges should store the FIRST (original) min-weight edges of each used augmenting path
            Map<Edge<String>, Double> maxFLow = network.maximizeFlow(network.getZeroFlow(), augmentingMinEdges);
            Set<Edge<String>> augMinEdgesOriginals = augmentingMinEdges.stream().map(e -> {
                return ((AugmentingEdge<String>) e).getOriginalEdge();
            }).collect(Collectors.toSet());

            //network.getAugmentingNetwork(maxFLow) SHOULD return the same aug. Network as in the last iteration of maximizeFlow
            AugmentingNetwork<String> augmentingNetwork = AugmentingNetwork.of(graph, maxFLow);
            Set<Node<String>> reachableNodes = augmentingNetwork.getReachableNodes(augmentingNetwork.getS()).stream().map(n -> {
                return network.getNode(n.getValue());
            }).collect(Collectors.toSet());

            Set<Edge<String>> reachableEdges = reachableNodes.stream().flatMap(n -> {
                //Use the outgoing edges of the corresponding node of n in network (NOT aug. network)
                return n.getOutgoingEdges().stream().filter(e -> {
                    return  !reachableNodes.contains(e.getTo());
                });
            }).collect(Collectors.toSet());
            if (augMinEdgesOriginals.containsAll(reachableEdges)) System.out.println("Reachable edges subset of augMinEdgesOriginals");
            if (reachableEdges.containsAll(augMinEdgesOriginals)) System.out.println("augMinEdgesOriginals subset of reachable edges");
            System.out.println("---------------------------------------------");
            //assertEquals(augMinEdgesOriginals, reachableEdges);
            //assertTrue(reachableEdges.containsAll(augMinEdgesOriginals) && augMinEdgesOriginals.containsAll(reachableEdges));
        }
    }

}