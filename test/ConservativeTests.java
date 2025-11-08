import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

public class ConservativeTests {

    private static final Random random = new Random(35903982591067L);

    private DirectedGraph<String, Edge<String>> graph;

    private static Stream<Arguments> getRandomGraphs() {
        //Network<String, Edge<String>> network = ;
        //return Stream.of(Arguments.of(network));
        return Stream.iterate(0, n -> n).map(n -> Arguments.of(GraphUtils.generateRandomDirectedGraph(
                150,          // number of nodes
                0.5,        // probability of edge
                random,         // seed for reproducibility
                (i, nodeCount) -> (i == 0 ? "S" : (i == nodeCount -1 ? "T" : "V" + i)),
                (random, edge) -> Math.round((random.nextDouble(7.0) - 5.0) * 100.0) / 100.0,
                true, true
        ))).limit(10);
    }

    @BeforeEach
    void setup() {
        graph = new DirectedGraph<>();
    }


    @Test
    void testAcyclicShortestPathExample() {
        graph.addEdge("S", "T", 10.0);
        graph.addEdge("S", "V2", 2.0);
        graph.addEdge("S", "V3", 7.0);
        graph.addEdge("S", "V4", 8.0);
        graph.addEdge("V2", "V3", 3.0);
        graph.addEdge("V2", "T", 9.0);
        graph.addEdge("V3", "V4", 4.0);
        graph.addEdge("V3", "T", 6.0);
        graph.addEdge("V4", "T", 5.0);

        Path<String> sp = graph.acyclicShortestPathSequential("S", "T");
        Path<String> sp_threads = graph.acyclicShortestPathThreads("S", "T", 9);
        System.out.println(sp);
        System.out.println(sp_threads);
    }

    @ParameterizedTest
    @MethodSource("getRandomGraphs")
        //@RepeatedTest(10)
    void testConservativeShortestPath(DirectedGraph<String, Edge<String>> graph) {
        Path<String> sp = graph.shortestConservativePath("S", "T", DirectedGraph.ExecutionStrategy.Sequential);
        if (sp != null) {
            System.out.println("Shortest Path is " + sp + "with " + sp.getEdges().size() + " edges.");
        } else {
            System.out.println("No path.");
        }
    }
    private static void measureAndPrint(String name, Runnable task) {
        long start = System.nanoTime();
        task.run();
        long durationNs = System.nanoTime() - start;
        double durationMs = durationNs / 1_000_000.0;
        System.out.printf("%-30s took %8.3f ms%n", name, durationMs);
    }
    @ParameterizedTest
    @MethodSource("getRandomGraphs")
    void testDifferentStrategies(DirectedGraph<String, Edge<String>> graph) {
        Path<String> sp = graph.shortestConservativePath("S", "T", DirectedGraph.ExecutionStrategy.ForkJoinPool);
        if (sp != null) {
            System.out.println("Shortest Path is " + sp + "with " + sp.getEdges().size() + " edges.");
        } else {
            System.out.println("No path.");
        }
        Path<String> sp_threads = graph.shortestConservativePath("S", "T", DirectedGraph.ExecutionStrategy.ExecutorService);
        if (sp_threads != null) {
            System.out.println("Shortest Path is " + sp_threads + "with " + sp_threads.getEdges().size() + " edges.");
        } else {
            System.out.println("No path.");
        }
    }

    @Test
    void testOnZU2() {
        graph.addEdge("S", "V5", -1.0);
        graph.addEdge("S", "V3", 3.0);
        graph.addEdge("S", "V2", 2.0);
        graph.addEdge("V2", "T", 3.0);
        graph.addEdge("V3", "T", 1.0);
        graph.addEdge("T", "V4", -4.0);
        graph.addEdge("V4", "V2", 1.0);

        Path<String> sp = graph.shortestConservativePath("S", "T", DirectedGraph.ExecutionStrategy.Sequential);
        if (sp != null) {
            System.out.println("Shortest Path is " + sp + "with " + sp.getEdges().size() + " edges.");
        } else {
            System.out.println("No path.");
        }
    }

    @Test
    void testOnZU2Mod() {
        graph.addEdge("S", "V5", -1.0);
        graph.addEdge("S", "V3", 3.0);
        graph.addEdge("S", "V2", 2.0);
        graph.addEdge("V2", "T", 1.0);
        graph.addEdge("V3", "T", 1.0);
        graph.addEdge("T", "V4", -4.0);
        graph.addEdge("V4", "V2", 1.0);

        Path<String> sp = graph.shortestConservativePath("S", "T", DirectedGraph.ExecutionStrategy.Sequential);
        if (sp != null) {
            System.out.println("Shortest Path is " + sp + "with " + sp.getEdges().size() + " edges.");
        } else {
            System.out.println("No path.");
        }
    }
}
