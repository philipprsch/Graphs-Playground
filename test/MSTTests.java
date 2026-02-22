import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MSTTests {
    private static final Random random = new Random(35903982591067L);

    private DirectedGraph<String, Edge<String>> graph;

    private static Stream<Arguments> getRandomGraphs() {
        //Network<String, Edge<String>> network = ;
        //return Stream.of(Arguments.of(network));
        return Stream.iterate(0, n -> n).map(n -> Arguments.of(GraphUtils.generateRandomUndirectedGraph(
                300,          // number of nodes
                0.6,        // probability of edge
                random,         // seed for reproducibility
                (i, nodeCount) -> (i == 0 ? "S" : (i == nodeCount -1 ? "T" : "V" + i)),
                (random, edge) -> Math.round((random.nextDouble(7.0)) * 100.0) / 100.0,
                false
        ))).limit(10);
    }
    private static Stream<Arguments> getRandomSmallGraphs() {
        //Network<String, Edge<String>> network = ;
        //return Stream.of(Arguments.of(network));
        return Stream.iterate(0, n -> n).map(n -> Arguments.of(GraphUtils.generateRandomUndirectedGraph(
                5,          // number of nodes
                0.6,        // probability of edge
                random,         // seed for reproducibility
                (i, nodeCount) -> (i == 0 ? "S" : (i == nodeCount -1 ? "T" : "V" + i)),
                (random, edge) -> Math.round((random.nextDouble(7.0)) * 100.0) / 100.0,
                false
        ))).limit(10);
    }
    private static Stream<Arguments> getRandomSameWeightGraphs() {
        return Stream.iterate(0, n -> n).map(n -> Arguments.of(GraphUtils.generateRandomUndirectedGraph(
                30,          // number of nodes
                0.8,        // probability of edge
                random,         // seed for reproducibility
                (i, nodeCount) -> (i == 0 ? "S" : (i == nodeCount -1 ? "T" : "V" + i)),
                (random, edge) -> 3.0,
                false
        ))).limit(10);
    }

    @BeforeEach
    void setup() {
        graph = new DirectedGraph<>();
    }


    @ParameterizedTest
    @MethodSource("getRandomSmallGraphs")
    public void testBoruvka(UndirectedGraph<String, UndirectedEdge<String>> graph) {
        UndirectedGraph<String, UndirectedEdge<String>> mstBoruvka = graph.boruvkasMST();

        Helpers.copyToClipboard(
                GraphUtils.combineIntoClusteredGraph(
                        List.of(GraphUtils.toGraphviz(graph),
                                GraphUtils.toGraphviz(mstBoruvka)),
                        List.of("Original",
                                "Boruvka")));

        assertFalse(mstBoruvka.hasCircle());
        System.out.println(mstBoruvka.getTotalWeight());
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
    public void benchmarkMSTMethods(UndirectedGraph<String, UndirectedEdge<String>> graph) {
        measureAndPrint("Boruvka", graph::boruvkasMST);
        measureAndPrint("Normal", graph::greedyMST);
    }
    @ParameterizedTest
    @MethodSource("getRandomGraphs")
    public void testMSTMethods(UndirectedGraph<String, UndirectedEdge<String>> graph) {
        UndirectedGraph<String, UndirectedEdge<String>> mstBoruvka = graph.boruvkasMST();
        UndirectedGraph<String, UndirectedEdge<String>> mstNormal = graph.greedyMST();

        Helpers.copyToClipboard(
                GraphUtils.combineIntoClusteredGraph(
                        List.of(GraphUtils.toGraphviz(graph),
                                GraphUtils.toGraphviz(mstBoruvka),
                                GraphUtils.toGraphviz(mstNormal)),
                        List.of("Original",
                                "Boruvka",
                                "Greedy")));

        assertEquals(mstNormal.getTotalWeight(), mstBoruvka.getTotalWeight(), 0.0001);
        assertFalse(mstNormal.hasCircle());
        assertFalse(mstBoruvka.hasCircle());

    }

    @ParameterizedTest
    @MethodSource("getRandomSameWeightGraphs")
    public void testMSTSameWeight(UndirectedGraph<String, UndirectedEdge<String>> graph) {
        UndirectedGraph<String, UndirectedEdge<String>> mstBoruvka = graph.boruvkasMST();
        UndirectedGraph<String, UndirectedEdge<String>> mstNormal = graph.greedyMST();

        Helpers.copyToClipboard(
                GraphUtils.combineIntoClusteredGraph(
                        List.of(GraphUtils.toGraphviz(graph),
                                GraphUtils.toGraphviz(mstBoruvka),
                                GraphUtils.toGraphviz(mstNormal)),
                        List.of("Original",
                                "Boruvka",
                                "Greedy")));


        assertFalse(mstNormal.hasCircle());
        assertFalse(mstBoruvka.hasCircle());
        assertEquals(mstNormal.getTotalWeight(), mstBoruvka.getTotalWeight(), 0.0001);


    }








}
