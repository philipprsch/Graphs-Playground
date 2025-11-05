import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class GraphUtils {

    public static class GraphMapping<T, E extends Edge<T>, U, F extends Edge<U>, G extends Graph<U, F>> {

        private final Map<Node<T>, Node<U>> nodeMapping;
        private final Map<E, F> edgeMapping;

        private final G newGraph;

        public GraphMapping(Map<Node<T>, Node<U>> nodeMapping, Map<E, F> edgeMapping, G newGraph) {
            this.nodeMapping = nodeMapping;
            this.edgeMapping = edgeMapping;
            this.newGraph = newGraph;
        }

        public Map<Node<T>, Node<U>> getNodeMapping() {
            return nodeMapping;
        }

        public Map<E, F> getEdgeMapping() {
            return edgeMapping;
        }

        public G getNewGraph() {
            return newGraph;
        }
    }

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
    GraphMapping<T, E, U, F, G> mapGraph(
            Graph<T, E> original,
            Function<T, U> nodeMapper,
            BiFunction<E, Map<Node<T>, Node<U>>, F> edgeMapper, //Provide Map of old to new nodes for edgeMapper Implementation
            Supplier<G> graphSupplier,
            TriConsumer<G, Map<Node<T>, Node<U>>, Map<E, F>> transformer
    ) {
        G newGraph = graphSupplier.get();
        Map<Node<T>, Node<U>> nodeMapping = new HashMap<>();

        // First, add all transformed nodes
        for (Node<T> oldNode : original.getNodes()) {
            U newValue = nodeMapper.apply(oldNode.getValue());
            Node<U> newNode = newGraph.addNode(newValue);
            nodeMapping.put(oldNode, newNode);
        }

        Map<E, F> edgeMapping = new HashMap<>();
        // Then, add all transformed edges
        for (E oldEdge : original.getEdges()) {
            F newEdge = edgeMapper.apply(oldEdge, nodeMapping);
            edgeMapping.put(oldEdge, newEdge);
            if (newEdge != null) newGraph.addExistingEdge(newEdge); // careful: this should preserve structure
        }
        transformer.accept(newGraph, nodeMapping, edgeMapping);

        return new GraphMapping<>(nodeMapping, edgeMapping, newGraph);
    }


    public static class NetworkNodeComparator<T> implements Comparator<Node<T>> {

        @Override
        public int compare(Node<T> aN, Node<T> bN) {
            if (!(aN.getValue() instanceof String) || !(bN.getValue() instanceof String)) {
                return 0;
            }
            String a = (String) aN.getValue();
            String b = (String) bN.getValue();
            // Handle nulls safely (optional)
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;

            // "S" should always come first
            if (a.equals("S") && !b.equals("S")) return -1;
            if (b.equals("S") && !a.equals("S")) return 1;

            // "T" should always come last
            if (a.equals("T") && !b.equals("T")) return 1;
            if (b.equals("T") && !a.equals("T")) return -1;

            // Handle "V" prefixed strings with numeric suffix
            if (a.startsWith("V") && b.startsWith("V")) {
                try {
                    int numA = Integer.parseInt(a.substring(1));
                    int numB = Integer.parseInt(b.substring(1));
                    return Integer.compare(numA, numB);
                } catch (NumberFormatException e) {
                    // Fall back to normal lexicographic comparison
                    return a.compareTo(b);
                }
            }

            // If one is "V..." and the other is not, put V’s before non-V’s (optional)
            if (a.startsWith("V") && !b.startsWith("V")) return -1;
            if (b.startsWith("V") && !a.startsWith("V")) return 1;

            // Default lexicographic comparison
            return a.compareTo(b);
        }
    }
    public static <T, E extends Edge<T> & GraphvizComponent> String toGraphviz(Graph<T, E> graph) {
        return toGraphviz(graph, (e) -> "", null);
    }

    public static <T, E extends Edge<T> & GraphvizComponent> String toGraphviz(Graph<T, E> graph, Function<E, String> extraEdgeInfo) {
        return toGraphviz(graph, extraEdgeInfo, null);
    }
    public static <T, E extends Edge<T> & GraphvizComponent> String toGraphviz(Graph<T, E> graph, Comparator<Node<T>> comparator) {
        return toGraphviz(graph,  (e) -> "", comparator);
    }

    public static <T, E extends Edge<T> & GraphvizComponent> String toGraphviz(Graph<T, E> graph, Function <E, String> extraEdgeInformer, Comparator<Node<T>> comparator) {
        boolean directed = graph instanceof DirectedGraph;

        StringBuilder sb = new StringBuilder();
        sb.append(directed ? "digraph" : "graph").append(" G {\n");



//        sb.append("  /* Layout + style */\n" +
//                "  rankdir=LR;        // lay out left-to-right\n" +
//                "  splines=line;      // straight edges (no curved splines)\n" +
//                "  overlap=false;\n" +
//                "  nodesep=0.6;       // horizontal spacing between nodes\n" +
//                "  ranksep=0.8;       // vertical spacing between ranks (not very relevant here)\n" +
//                "\n" +
//                "  /* Node / edge defaults */\n" +
//                "  node [shape=circle, style=filled, fillcolor=lightgray, fontname=\"Helvetica\"];\n" +
//                "  edge [arrowhead=normal, fontsize=10, fontname=\"Helvetica\"];");


        String connector = directed ? "->" : "--";

        // Write nodes (optional if you want them even without edges)
        List<Node<T>> nodeCollection = (comparator != null ?
                graph.getNodes().stream().sorted(comparator).toList() :
                graph.getNodes().stream().toList());

//        sb.append("{ rank=s; ");
//
//        // Join all nodes with " -> "
//        int i = 0;
//        for (Node<T> node : nodeCollection) {
//            sb.append('"').append(node.getValue().toString()).append('"');
//            if (i < nodeCollection.size() - 1) {
//                sb.append(" -> ");
//            }
//            i++;
//        }
//
//        // Append style attributes
//        sb.append(" [style=invis, weight=10]; }\n");

        for (int i = 0; i < nodeCollection.size() - 1; i++) {
            String from = nodeCollection.get(i).getValue().toString();
            String to = nodeCollection.get(i + 1).getValue().toString();
            sb.append('"').append(from).append('"')
                    .append(" -> ")
                    .append('"').append(to).append('"')
                    .append(" [weight=10, style=invis];\n");
        }

        //Declare nodes:

        for (Node<T> node : nodeCollection) {
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
                    .append("[label=\"").append(escape(edge.toGraphviz() + " " + extraEdgeInformer.apply(edge))).append("\"];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /** Escape double quotes and backslashes for Graphviz safety. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Combines multiple DOT subgraphs into a single boxed graph with labels.
     *
     * @param subgraphsInput a list of DOT strings representing smaller graphs (without digraph headers)
     * @param labels a list of labels corresponding to each subgraph
     * @return a DOT string representing the composed graph
     */
    public static String combineIntoClusteredGraph(List<String> subgraphsInput, List<String> labels) {

        //Remove Digraph headers for subgraphs
        List<String> subgraphs = new LinkedList<>(subgraphsInput);
//        for (String subgraph : subgraphsInput) {
//            String inner = subgraph
//                    .replaceFirst("(?is)^\\s*digraph\\s+\\w*\\s*\\{", "") // remove "digraph G1 {"
//                    .replaceFirst("(?s)\\}$", "")                         // remove trailing "}"
//                    .trim();
//            subgraphs.add(inner);
//        }



        if (subgraphs.size() != labels.size()) {
            throw new IllegalArgumentException("subgraphs and labels must have the same size");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph CombinedGraph {\n");
//        sb.append("  rankdir=LR;\n");
//        sb.append("  ranksep=1.0;\n");
//        sb.append("  compound=true;\n");
//        sb.append("  splines=true;\n");
//        sb.append("  overlap=false;\n");

        sb.append("layout=dot;\n" +
                "  rankdir=LR;\n" +
                "  splines=true;\n" +
                "  overlap=false;\n" +
                "  concentrate=false;\n" +
                "  ranksep=1.2;\n" +
                "  nodesep=0.6;\n");

        sb.append("  node [shape=circle, style=filled, fillcolor=lightgray, fontname=\"Helvetica\"];\n");

        for (int i = 0; i < subgraphs.size(); i++) {
            String label = labels.get(i);
            String inner = subgraphs.get(i)
                    .replaceFirst("(?i)^\\s*digraph\\s+\\w+\\s*\\{", "")  // remove digraph header
                    .replaceFirst("(?s)\\}$", "");                        // remove final closing brace

            inner = inner.replaceAll("\"([A-Za-z0-9_]+)\"", "\"" + i + "_$1\""); //Prefix nodes

            sb.append("  subgraph cluster_").append(i).append(" {\n");
//            sb.append("    rankdir=LR;\n");
//            sb.append("    label=\"").append(label).append("\";\n");
//            sb.append("    style=rounded;\n");
//            sb.append("    color=gray;\n");
//            sb.append("    fontname=\"Helvetica\";\n");
//            sb.append("    fontsize=12;\n");
//            sb.append("    ordering=\"in\";\n\n");

            sb.append("label=\""+label+"\";\n" +
                    "    style=rounded;\n" +
                    "    color=gray;\n" +
                    "\n" +
                    "    // Fix source on the far left\n" +
                    "    { rank=min; \""+i+"_S\"; }\n" + //Start is alway node with string S
                    "\n" +
                    "    // Fix sink on the far right\n" +
                    "    { rank=max; \""+i+"_T\"; }"); //Sink is always node with String T

            sb.append(indent(inner.trim(), "    ")).append("\n");
            sb.append("  }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String indent(String text, String prefix) {
        return text.replaceAll("(?m)^", prefix);
    }

    public static <T> Network<T, Edge<T>> generateRandomNetwork(
            int nodeCount,
            double edgeProbability,
            Random random,
            double forwardBias,
            BiFunction<Integer, Integer, T> nodeValueFactory
    ) {
        //Random random = new Random(seed);
        Network<T, Edge<T>> graph = new Network<>();

        // --- Create nodes ---
        Node<T> source = graph.addNode(nodeValueFactory.apply(0, nodeCount)); // "S"
        Node<T> sink = graph.addNode(nodeValueFactory.apply(nodeCount - 1, nodeCount)); // "T"
        graph.setS(source);
        graph.setT(sink);

        List<Node<T>> nodes = new ArrayList<>();
        nodes.add(source);
        for (int i = 1; i < nodeCount - 1; i++) {
            nodes.add(graph.addNode(nodeValueFactory.apply(i, nodeCount)));
        }
        nodes.add(sink);

        // --- Add edges ---
        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (i == j) continue; // no self-loops

                Node<T> from = nodes.get(i);
                Node<T> to = nodes.get(j);

                // Enforce unique edges
                if (graph.hasEdge(from, to)) continue;
                if (graph.hasEdge(to, from)) continue;

                // Basic probability check
                if (random.nextDouble() < edgeProbability) {

                    // Bias towards forward direction (S → T)
                    boolean isForward = random.nextDouble() < forwardBias || i < j;
                    if (!isForward) {
                        Node<T> tmp = from;
                        from = to;
                        to = tmp;
                    }

                    double weight = 1 + random.nextInt(10); // random weight 1–10
                    graph.addEdge(from.getValue(), to.getValue(), weight);
                }
            }
        }

        return graph;
    }

    public static <T> DirectedGraph<T, Edge<T>> generateRandomDirectedGraph(
            int nodeCount,
            double edgeProbability,
            Random random,
            BiFunction<Integer, Integer, T> nodeValueFactory,
            BiFunction<Random, Edge<T>, Double> weightFactory,
            boolean selfLoops,
            boolean biDirection
    ) {
        DirectedGraph<T, Edge<T>> graph = new DirectedGraph<>();

        List<Node<T>> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(graph.addNode(nodeValueFactory.apply(i, nodeCount)));
        }

        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (!selfLoops && i == j) continue; // no self-loops

                Node<T> from = nodes.get(i);
                Node<T> to = nodes.get(j);

                // Enforce unique edges
                if (selfLoops && from == to) continue; // from = to => selfLoops (see above if)
                if (!selfLoops && graph.hasEdge(from, to)) continue;
                if (!selfLoops && !biDirection && graph.hasEdge(to, from)) continue;

                // Basic probability check
                if (random.nextDouble() < edgeProbability) {
                    Edge<T> newEdge = new Edge<T>(from, to);
                    double weight = weightFactory.apply(random, newEdge);
                    newEdge.setWeight(weight);
                    graph.addExistingEdge(newEdge);
                }
            }
        }

        return graph;
    }

}