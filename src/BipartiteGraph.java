import java.util.*;

public class BipartiteGraph<T, E extends Edge<T>> {
    private final Graph<T, E> graph;
    private final Set<Node<T>> leftPartition = new HashSet<>();
    private final Set<Node<T>> rightPartition = new HashSet<>();

    public BipartiteGraph(Graph<T, E> graph) {
        this.graph = graph;
    }


    public void addToLeft(Node<T> node) {
        if (rightPartition.contains(node))
            throw new IllegalArgumentException("Node already in right partition");
        leftPartition.add(node);
    }

    public void addToRight(Node<T> node) {
        if (leftPartition.contains(node))
            throw new IllegalArgumentException("Node already in left partition");
        rightPartition.add(node);
    }

    public boolean isLeft(Node<T> node) { return leftPartition.contains(node); }
    public boolean isRight(Node<T> node) { return rightPartition.contains(node); }

    public Graph<T, E> getGraph() { return graph; }

    /** Checks if this graph is truly bipartite according to the partitions. */
    public boolean validateBipartite() {
        for (E edge : graph.getEdges()) {
            Node<T> from = edge.getFrom();
            Node<T> to = edge.getTo();
            boolean fromLeft = leftPartition.contains(from);
            boolean fromRight = rightPartition.contains(from);
            boolean toLeft = leftPartition.contains(to);
            boolean toRight = rightPartition.contains(to);

            // Edge must connect nodes in opposite partitions
            if ((fromLeft && toLeft) || (fromRight && toRight)) {
                return false;
            }

            // Ensure both endpoints exist in partitions
            if (!(fromLeft || fromRight) || !(toLeft || toRight)) {
                return false;
            }
        }
        return true;
    }

    public boolean existsLeftCoveringMatching() {
        return Helpers.powerSet(new LinkedList<>(leftPartition)).stream().noneMatch(M -> {
            return (Graph.neighbours(new HashSet<>(M)).size() < M.size());
        });
    }

    public Map<T, T> getMaximalMatching() {
        Map<T, T> matching = new HashMap<>();

        Object source = new Object() {
            @Override
            public String toString() {
                return "Source";
            }
        };
        Object drain = new Object() {
            @Override
            public String toString() {
                return "Drain";
            }
        };

        GraphUtils.GraphMapping<T, E, Object, Edge<Object>, Network<Object, Edge<Object>>> networkMapping =
                GraphUtils.<T, E, Object, Edge<Object>, Network<Object, Edge<Object>>>mapGraph(
                    this.graph,
                    (n) -> {
                        return (Object) n;
                    },
                    (e, nodeMapping) -> {
                        return new Edge<Object>(nodeMapping.get(e.getFrom()), nodeMapping.get(e.getTo()), Double.MAX_VALUE);
                    },
                    Network::new,
                    (newGraph, nodeMap, edgeMap) -> {
                        newGraph.setS(source);
                        newGraph.setT(drain);
                    }
        );
        Network<Object, Edge<Object>> network = networkMapping.getNewGraph();
        Map<Node<T>, Node<Object>> networkNodeMap = networkMapping.getNodeMapping();

        //Add helper source from source to A Partition AND from B Partition to drain
        this.leftPartition.forEach(a -> {
            network.addEdge(source, networkNodeMap.get(a).getValue(), 1.0);
        });
        this.rightPartition.forEach(b -> {
            network.addEdge(networkNodeMap.get(b).getValue(), drain, 1.0);
        });

        Map<Edge<Object>, Double> flow = network.maximizeFlow();
        networkMapping.getEdgeMapping().entrySet().stream().filter((entry) -> {
            double myFlow = flow.get(entry.getValue());
            assert (myFlow == 1.0 || myFlow == 0.0) : "Flow is not elementary";
            return myFlow != 0.0;
        }).map(Map.Entry::getKey).forEach(e -> matching.put(e.getFrom().getValue(), e.getTo().getValue()));

        return matching;
    }


    @Override
    public String toString() {
        return "BipartiteGraph{\n" +
                "Left=" + leftPartition + ",\n" +
                "Right=" + rightPartition + "\n}";
    }

    /** Utility for constructing directly from two sets of node values */
    public static <T, E extends Edge<T>> BipartiteGraph<T, E> fromNodeSets(
            Graph<T, E> baseGraph,
            Collection<Node<T>> left,
            Collection<Node<T>> right) {
        BipartiteGraph<T, E> bg = new BipartiteGraph<>(baseGraph);
        left.forEach(bg::addToLeft);
        right.forEach(bg::addToRight);
        return bg;
    }
    public static <T, E extends Edge<T>> BipartiteGraph<T, E> fromValueSets(
            Graph<T, E> baseGraph,
            Collection<T> leftValues,
            Collection<T> rightValues) {
        BipartiteGraph<T, E> bg = new BipartiteGraph<>(baseGraph);

        for (T v : leftValues) {
            Node<T> n = baseGraph.addNode(v);
            bg.addToLeft(n);
        }
        for (T v : rightValues) {
            Node<T> n = baseGraph.addNode(v);
            bg.addToRight(n);
        }

        return bg;
    }
}