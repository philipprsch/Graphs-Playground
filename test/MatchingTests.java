import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchingTests {
    private DirectedGraph<String, Edge<String>> graph;
    private BipartiteGraph<String, Edge<String>> bipartiteGraphWrapper;

    @BeforeEach
    void setup() {
        graph = new DirectedGraph<>();
    }
    @Test
    void testMaximalCardinalityMatching() {
        Collection<String> leftPartition = List.of("S1", "S2", "S3", "S4");
        Collection<String> rightPartition = List.of("P1", "P2", "P3", "P4");

        //Add Platz (P) Preferences for each Student (S)
        graph.addEdge("S1", "P1", 3.0);
        graph.addEdge("S1", "P2", 3.0);
        graph.addEdge("S2", "P2", 3.0);
        graph.addEdge("S2", "P3", 1.0);
        graph.addEdge("S3", "P2", 1.0);
        graph.addEdge("S3", "P4", 1.0);
        graph.addEdge("S4", "P4", 3.0);

        //Create Bipartition
        bipartiteGraphWrapper = BipartiteGraph.fromValueSets(graph, leftPartition, rightPartition);
        Map<String, String> matching = bipartiteGraphWrapper.getMaximalMatching();
        System.out.println(matching);
    }
    @Test
    void testLeftCovExistenceAndAlgorithmCohesion() {
        int n = 4; // number of nodes on each side
        Random random = new Random();

        // Create left and right partitions
        List<String> leftPartition = new ArrayList<>();
        List<String> rightPartition = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            leftPartition.add("S" + i);
            rightPartition.add("P" + i);
        }

        for (int g = 0; g < 50; g++) {
            graph = new DirectedGraph<>();

            // Randomly add edges from left (S) to right (P)
            for (String s : leftPartition) {
                int numEdges = random.nextInt(n + 1); // random number of outgoing edges (0..n)
                Set<String> chosen = new HashSet<>();
                for (int i = 0; i < numEdges; i++) {
                    String p = rightPartition.get(random.nextInt(n));
                    chosen.add(p); // ensure no duplicate edges
                }
                for (String p : chosen) {
                    graph.addEdge(s, p);
                }
            }

            bipartiteGraphWrapper = BipartiteGraph.fromValueSets(graph, leftPartition, rightPartition);
            Map<String, String> matching = bipartiteGraphWrapper.getMaximalMatching();

            boolean exists = bipartiteGraphWrapper.existsLeftCoveringMatching();
            assertEquals(exists, matching.size() == leftPartition.size());
            System.out.println(exists);

        }


    }
}
