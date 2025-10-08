import java.util.*;
import java.util.function.*;

public class GraphUtils {

    /**
     * Creates a new graph from an existing one by applying transformation functions.
     *
     * @param original The original graph
     * @param nodeMapper Function to map old node values to new ones
     * @param edgeMapper Function to map old edges to new edge objects (can copy or modify)
     * @param <T> Source node value type
     * @param <E> Source edge type
     * @param <U> Target node value type
     * @param <F> Target edge type
     * @param <G> Graph type (Directed or Undirected)
     * @return New transformed graph
     */
    public static <T, E extends Edge<T>, U, F extends Edge<U>, G extends Graph<U, F>>
    G mapGraph(
            Graph<T, E> original,
            Function<T, U> nodeMapper,
            BiFunction<E, Map<Node<T>, Node<U>>, F> edgeMapper, //Provide Map of old to new nodes for edgeMapper Implementation
            Supplier<G> graphSupplier
    ) {
        G newGraph = graphSupplier.get();
        Map<Node<T>, Node<U>> nodeMapping = new HashMap<>();

        // First, add all transformed nodes
        for (Node<T> oldNode : original.getNodes()) {
            U newValue = nodeMapper.apply(oldNode.getValue());
            Node<U> newNode = newGraph.addNode(newValue);
            nodeMapping.put(oldNode, newNode);
        }

        // Then, add all transformed edges
        for (E oldEdge : original.getEdges()) {
            F newEdge = edgeMapper.apply(oldEdge, nodeMapping);
            if (newEdge != null) newGraph.addExistingEdge(newEdge); // careful: this should preserve structure
        }

        return newGraph;
    }

    public static <T, E extends Edge<T>> String toGraphviz(Graph<T, E> graph, Function <E, String> extraEdgeInformer) {
        boolean directed = graph instanceof DirectedGraph;

        StringBuilder sb = new StringBuilder();
        sb.append(directed ? "digraph" : "graph").append(" G {\n");

        String connector = directed ? "->" : "--";

        // Write nodes (optional if you want them even without edges)
        for (Node<T> node : graph.getNodes()) {
            sb.append("  \"")
                    .append(node.getValue().toString())
                    .append("\";\n");
        }

        // Write edges
        for (E edge : graph.getEdges()) {
            String from = edge.getFrom().getValue().toString();
            String to = edge.getTo().getValue().toString();

            sb.append("  \"").append(from).append("\" ")
                    .append(connector).append(" \"").append(to).append("\" ")
                    .append("[label=\"").append(escape(edge.toString() + " " + extraEdgeInformer.apply(edge))).append("\"];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /** Escape double quotes and backslashes for Graphviz safety. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}