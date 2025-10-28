import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MinIncreasementTests {
    private Network<String, Edge<String>> graph;

    private static final Random random = new Random(35903982591067L);

    private static Stream<Arguments> getRandomNetwork() {
        //Network<String, Edge<String>> network = ;
        //return Stream.of(Arguments.of(network));
        return Stream.iterate(0, n -> n).map(n -> Arguments.of(GraphUtils.generateRandomNetwork(
                15,          // number of nodes
                0.5,        // probability of edge
                random,         // seed for reproducibility
                0.9,         // bias (0.8 = 80% edges forward)
                (i, nodeCount) -> (i == 0 ? "S" : (i == nodeCount -1 ? "T" : "V" + i))
        ))).limit(10);
    }

    @BeforeEach
    void setup() {
        graph = new Network<>("S", "T");
    }

    @Test
    void testMinIncProducesFlowOfK() {

        graph.addEdge("S", "V2", 4.0);
        graph.addEdge("S", "V1", 5.0);
        graph.addEdge("V2", "V4", 1.0);
        graph.addEdge("V2", "V1", 1.0);
        graph.addEdge("V2", "V3", 1.0);
        graph.addEdge("V1", "V4", 7.0);
        graph.addEdge("V1", "V3", 4.0);
        graph.addEdge("V3", "T", 2.0);
        graph.addEdge("V4", "T", 4.0);

        double K = 15.5;

        Map<Edge<String>, Double> inc = graph.getMinimalCapacityIncreasement(K, graph.getZeroFlow());

        Supplier<Stream<Map.Entry<Edge<String>, Double>>> incStreamSup = () -> inc.entrySet().stream().filter(i -> i.getValue() > 0);
        System.out.println("Result: " + incStreamSup.get().count() + " Edges must be upgraded by a total of " +
                incStreamSup.get().map(Map.Entry::getValue).reduce(Double::sum).orElse(0.0) +
                " capacity units to meet flow value: " + K);

        String incString = incStreamSup.get().map(entry -> "Edge: " + entry.getKey().toString() + " Inc: " + entry.getValue() + "\n").reduce(String::concat).orElse("No Edges.");
        System.out.println(incString);

        //Create a new test garph with edgeWeight = original + increasement
        GraphUtils.GraphMapping<String, Edge<String>, String, Edge<String>, Network<String, Edge<String>>> testGraphMapping = GraphUtils.mapGraph(
                graph,
                Function.identity(),
                (edge, nodeMap) -> {
                    return new Edge<String>(nodeMap.get(edge.getFrom()), nodeMap.get(edge.getTo()), edge.getWeight() + inc.get(edge));
                },
                Network::new,
                (newGraph, nodeMapping, edgeMapping) -> {
                    newGraph.setS(nodeMapping.get(graph.getS()));
                    newGraph.setT(nodeMapping.get(graph.getT()));
                }
        );
        Network<String, Edge<String>> testGraph = testGraphMapping.getNewGraph();
        //maximize flow on test graph and check flowValue == K
        Map<Edge<String>, Double> flow = testGraph.maximizeFlow();
        assertEquals(K, testGraph.flowValue(flow));
    }

    private void printIncreasement(Map<Edge<String>, Double> inc) {

    }

    @ParameterizedTest
    @MethodSource("getRandomNetwork")
    //@RepeatedTest(10)
    void testMinIncForFlowKIsIndeedMin(Network<String, Edge<String>> graph) {
//        graph.addEdge("S", "V2", 4.0);
//        graph.addEdge("S", "V1", 5.0);
//        graph.addEdge("V2", "V4", 1.0);
//        graph.addEdge("V2", "V1", 1.0);
//        graph.addEdge("V2", "V3", 1.0);
//        graph.addEdge("V1", "V4", 7.0);
//        graph.addEdge("V1", "V3", 4.0);
//        graph.addEdge("V3", "T", 2.0);
//        graph.addEdge("V4", "T", 4.0);

        double K = 80.0;
        double graphFlow = graph.flowValue(graph.maximizeFlow());

        Map<Edge<String>, Double> inc = graph.getMinimalCapacityIncreasement(K, graph.getZeroFlow());
        if (inc == null) {
            System.out.println("There exists no increasement to satisfy flow K. Flow is always 0!");
            return;
        }

        Supplier<Stream<Map.Entry<Edge<String>, Double>>> incStreamSup = () -> inc.entrySet().stream().filter(i -> i.getValue() > 0);
        double incSumValue = incStreamSup.get().map(Map.Entry::getValue).reduce(Double::sum).orElse(0.0);

        System.out.println("Result: " + incStreamSup.get().count() + " Edges must be upgraded by a total of " +
                incSumValue + " capacity units to meet flow value: " + K);

        String incString = incStreamSup.get().map(entry -> "Edge: " + entry.getKey().toString() + " Inc: " + entry.getValue() + "\n").reduce(String::concat).orElse("No Edges.");
        System.out.println(incString);

        System.out.println("Now let's check if the above increasement is indeed minimal...");

        List<Edge<String>> edges = graph.getEdges();
        int k = edges.size() + 1; //Add a no-edge class for unassigned flow units;
        Helpers.traverseNonDecTuplesNonRec((int) incSumValue, k, (inc_flow_unit_tuple) -> {
            ArrayList<Integer> inc_edge_tuple = Helpers.CknToDnk(inc_flow_unit_tuple, k);

            System.out.println(inc_flow_unit_tuple);
            System.out.println(inc_edge_tuple);

            double altIncSumValue = (double) inc_flow_unit_tuple.stream().limit(edges.size()).reduce(Integer::sum).orElse(0); //0 = no edges

            //Skip redundant altInc test cases : altInc Increasement Sum Value same as inc
            if (altIncSumValue == incSumValue) return 0;

            //Skip redundant altInc test cases : altInc Increasement Sum Value (proven) too low to produce flow K
            if (altIncSumValue < K - graphFlow) return 0;

            Map<Edge<String>, Double> altInc = graph.getZeroFlow();
            for (int i = 0; i < k - 1; i++) { //Ignore not assigned flow units
                altInc.put(edges.get(i), Double.valueOf(inc_edge_tuple.get(i)));
            }
            //Test if altInc produces sufficient flow K
            GraphUtils.GraphMapping<String, Edge<String>, String, Edge<String>, Network<String, Edge<String>>> testGraphMapping = GraphUtils.mapGraph(
                    graph,
                    Function.identity(),
                    (edge, nodeMap) -> {
                        return new Edge<String>(nodeMap.get(edge.getFrom()), nodeMap.get(edge.getTo()), edge.getWeight() + altInc.get(edge));
                    },
                    Network::new,
                    (newGraph, nodeMapping, edgeMapping) -> {
                        newGraph.setS(nodeMapping.get(graph.getS()));
                        newGraph.setT(nodeMapping.get(graph.getT()));
                    }
            );
            Network<String, Edge<String>> testGraph = testGraphMapping.getNewGraph();
            //maximize flow on test graph and check if (altInc produces) flowValue >= K
            Map<Edge<String>, Double> altFlow = testGraph.maximizeFlow();
            double altFLowValue = testGraph.flowValue(altFlow);

            assertTrue(altFLowValue <= K, ""); //actually should even be strictly smaller

            return 0;
        });

    }
}
