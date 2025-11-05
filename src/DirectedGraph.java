import javax.xml.stream.events.EndDocument;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DirectedGraph<T, E extends Edge<T>> extends Graph<T, E> {

    private static final Logger LOG = Logger.getLogger(DirectedGraph.class.getName());

    public DirectedGraph() {
        super();
        LOG.setLevel(Level.ALL);
    }

    public DirectedGraph(EdgeFactory<T, E> edgeFactory) {
        super(edgeFactory);
    }

    @Override
    public void addExistingEdge(E edge) {
        edges.put(new EdgeKey(edge.getFrom(), edge.getTo()), edge);
        edge.getFrom().addOutgoingEdge((E) edge);
        edge.getTo().addIncomingEdge((E) edge);
    }

    @Override
    public void removeEdge(E edge) {
        EdgeKey key = new EdgeKey(edge.getFrom(), edge.getTo());

        //System.out.println("Before remove: " + edges.get(key).toString() + " Removing: " + edge.toString());
        if (!edges.remove(key, edge)) {
            throw new RuntimeException("Removing edge failed, wrong mapping");
        }
        edge.getFrom().removeOutgoingEdge(edge);
        edge.getTo().removeIncomingEdge(edge);
    }

    //Get the shortest - in terms of summed edge weights - path in a know to be ACYCLIC graph

    private final Map<EdgeKey, Path<T>> ASPMemory = Collections.synchronizedMap(new HashMap<>());


    public Path<T> acyclicShortestPath(Node<T> startNode, Node<T> endNode, int debug_depth) {
        if (!nodes.containsKey(startNode.getValue()) || !nodes.containsKey(endNode.getValue())) throw new IllegalArgumentException("Node not part of Graph");

        if (startNode.equals(endNode)) return Path.empty();

        EdgeKey pathKey = new EdgeKey(startNode, endNode);

        //System.out.println("\t".repeat(debug_depth) + "Path: " + startNode + " -> " + endNode + (ASPMemory.containsKey(pathKey) ? " MEMORY" : ""));
        //Use containsKey to return null only if we mapped pathKey to null <=>
        //we have previously computed, that no path from S to T exists
        if (ASPMemory.containsKey(pathKey)) {
            return ASPMemory.get(pathKey);
        }

        //if (endNode.getIncomingEdges().isEmpty()) return null;


        //LOG.finest("\t".repeat(debug_depth) + "Path: " + startNode + " -> " + endNode);

        List<Path<T>> extIncomingPaths = new LinkedList<>();

        for (Edge<T> incoming: endNode.getIncomingEdges()) {
            Path<T> incomingPath = acyclicShortestPath(startNode, incoming.getFrom(), debug_depth + 1);
            if (incomingPath != null) extIncomingPaths.add(incomingPath.withEdge(incoming));
        }

        Path<T> shortestPath = extIncomingPaths.stream().min(Comparator.comparingDouble(Path::getTotalWeight)).orElse(null);
        ASPMemory.put(pathKey, shortestPath);

        return shortestPath;

    }

//    class AcyclicShortestPathTask extends RecursiveAction {
//        private final int[] arr;
//        private final int start, end;
//        private static final int THRESHOLD = 500;
//
//        AcyclicShortestPathTask(int[] arr, int start, int end) {
//            this.arr = arr; this.start = start; this.end = end;
//        }
//
//        @Override
//        protected void compute() {
//            if (end - start <= THRESHOLD) {
//                Arrays.sort(arr, start, end);
//                return;
//            }
//            int mid = (start + end) / 2;
//            MergeSortTask left = new MergeSortTask(arr, start, mid);
//            MergeSortTask right = new MergeSortTask(arr, mid, end);
//            invokeAll(left, right); // forks both, joins automatically
//            merge(start, mid, end);
//        }
//
//        private void merge(int start, int mid, int end) { /* ... */ }
//    }

    public Path<T> acyclicShortestPath(T startNode, T endNode) {
        return acyclicShortestPath(getNode(startNode), getNode(endNode), 0);
    }


    //Use BFS to determine the shortest - in terms of edges used - path between s and t (in any graph)
    public Path<T> shortestPath(Node<T> startNode, Node<T> endNode) {
        if (!nodes.containsKey(startNode.getValue()) || !nodes.containsKey(endNode.getValue())) throw new IllegalArgumentException("Node not part of Graph");

        LinkedList<Node<T>> queue = new LinkedList<>();
        Map<Node<T>, Path<T>> shortestPath = new HashMap<>();

        shortestPath.put(startNode, new Path<>());
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node<T> currentNode = queue.pop();
            for (Edge<T> outgoing : currentNode.getOutgoingEdges()) {
                Node<T> neighbour = outgoing.getTo();
                if (shortestPath.containsKey(neighbour)) continue;
                shortestPath.computeIfAbsent(neighbour, n -> shortestPath.get(currentNode).withEdge(outgoing));
                queue.add(neighbour);
            }
        }
        return shortestPath.get(endNode);
    }
    public Path<T> shortestPath(T startNode, T endNode) {
        return shortestPath(getNode(startNode), getNode(endNode));
    }
    public Set<Node<T>> getReachableNodes(Node<T> start) {
        if (!nodes.containsKey(start.getValue())) throw new IllegalArgumentException("Node not part of Graph");

        LinkedList<Node<T>> queue = new LinkedList<>();
        Set<Node<T>> visited = new HashSet<>();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node<T> currentNode = queue.pop();
            for (Edge<T> outgoing : currentNode.getOutgoingEdges()) {
                Node<T> neighbour = outgoing.getTo();
                if (visited.add(neighbour)) {
                    queue.add(neighbour);
                }

            }
        }
        return visited;
    }

    public boolean isConservative() {
        return false; //TODO: Implement this method
    }

    static class CHNValue<T> { //Conservative Helper (Graph) Node Value
        public final Node<T> node;
        public final Integer k;

        CHNValue(Node<T> node, int k) {
            this.node = node;
            this.k = k;
        }
        @Override
        public String toString() {
            return "(" + node.getValue().toString() + ", " + k + ")";
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DirectedGraph.CHNValue)) return false;
            CHNValue<T> key = (CHNValue<T>) o;
            return Objects.equals(node, key.node) &&
                    Objects.equals(k, key.k);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, k);
        }
    }

    private DirectedGraph<CHNValue<T>, Edge<CHNValue<T>>> getConservativeHelperGraph() {
        DirectedGraph<CHNValue<T>, Edge<CHNValue<T>>> chg = new DirectedGraph<>();
        int nodeCount = this.nodes.size();

        this.nodes.forEach((key, n) -> {
            for (int k = 0; k < nodeCount; k++) {
                chg.addNode(new CHNValue<>(n, k));
            }
        });

        this.edges.forEach((key, e) -> {
            for (int k = 0; k < nodeCount - 1; k++) {
                chg.addExistingEdge(new Edge<>(chg.getNode(new CHNValue<>(e.getFrom(), k)), chg.getNode(new CHNValue<>(e.getTo(), k + 1)), e.getWeight()));
            }
        });

        return chg;
    }

    //Get the shortest - in terms of summed edge weights - path from s to t in a known to be conservative graph (no negative weighted circles)
    public Path<T> shortestConservativePath(Node<T> s, Node<T> t) {

        int nodeCount = this.nodes.size();
        DirectedGraph<CHNValue<T>, Edge<CHNValue<T>>> chg = this.getConservativeHelperGraph();

        //Debugging stuff
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        graphs.add(GraphUtils.toGraphviz(this));
        graphs.add(GraphUtils.toGraphviz(chg));
        graphLabels.add("G"); graphLabels.add("H");

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));
        //Node<CHNValue<T>> chgS = chg.getNode();
        //Node<CHNValue<T>> chgT = chg.getNode(new CHNValue<>(t, 0));

/*
        Stream<Integer> ns = Stream.iterate(1, k -> k).limit(nodeCount - 1);
        List<Path<CHNValue<T>>> shortestKPaths = ns.map(k -> chg.acyclicShortestPath(new CHNValue<>(s, 0), new CHNValue<>(t, k))).toList();

*/
        List<Path<CHNValue<T>>> shortestKPaths = new LinkedList<>();
        for (int k = 1; k < nodeCount; k++) {
            shortestKPaths.add(chg.acyclicShortestPath(new CHNValue<>(s, 0), new CHNValue<>(t, k)));
        }
        Path<CHNValue<T>> chgSP = shortestKPaths.stream().filter(Objects::nonNull).min(Comparator.comparingDouble(Path::getTotalWeight)).orElse(null);

        if (chgSP == null) return null;
        return Path.of(chgSP.getEdges().stream().map(e -> this.getEdge(e.getFrom().getValue().node, e.getTo().getValue().node)));
    }

    public Path<T> shortestConservativePathThreads(Node<T> s, Node<T> t) {

        int nodeCount = this.nodes.size();
        DirectedGraph<CHNValue<T>, Edge<CHNValue<T>>> chg = this.getConservativeHelperGraph();

        //Debugging remove later
        chg.acyclicShortestPath(new CHNValue<>(s, 0), new CHNValue<>(t, 4));
        //End

        //Debugging stuff
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        graphs.add(GraphUtils.toGraphviz(this));
        graphs.add(GraphUtils.toGraphviz(chg));
        graphLabels.add("G"); graphLabels.add("H");

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));
        //Node<CHNValue<T>> chgS = chg.getNode();
        //Node<CHNValue<T>> chgT = chg.getNode(new CHNValue<>(t, 0));

/*
        Stream<Integer> ns = Stream.iterate(1, k -> k).limit(nodeCount - 1);
        List<Path<CHNValue<T>>> shortestKPaths = ns.map(k -> chg.acyclicShortestPath(new CHNValue<>(s, 0), new CHNValue<>(t, k))).toList();

*/
        Set<Path<CHNValue<T>>> shortestKPaths = Collections.synchronizedSet(new HashSet<>());
        Set<Integer> finishedKs = Collections.synchronizedSet(new HashSet<>());
        int p = Runtime.getRuntime().availableProcessors();
        //LOG.fine("Starting " + (nodeCount -1) + " k-path finding jobs on " + p + " processors");
        System.out.println("Starting " + (nodeCount -1) + " k-path finding jobs on " + p + " processors");
        try (ExecutorService exec = Executors.newFixedThreadPool(p)) {
            int[] bounds = Helpers.getSimilarExpLoadClassesBounds(0, nodeCount - 1, p, 1.0);

            String[] ranges = IntStream.range(0, bounds.length - 1)
                    .mapToObj(i -> "Processor " + i + ": " + (bounds[i] + 1) + " - " + bounds[i + 1])
                    .toArray(String[]::new);

            Arrays.stream(ranges).forEach(System.out::println);


            for (int cp = 0; cp < bounds.length - 1; cp++) { //(bounds.length - 1) = Math.min(p, nodeCount -1);
                int start = bounds[cp] + 1;
                int end = bounds[cp + 1];
                exec.submit(() -> {
                    for (int k = start; k <= end; k++) {
                        shortestKPaths.add(chg.acyclicShortestPath(new CHNValue<>(s, 0), new CHNValue<>(t, k)));
                        finishedKs.add(k);
                    }
                });
            }
            //exec.awaitTermination(1, TimeUnit.DAYS);
            exec.shutdown();
            boolean gracefulTermination = exec.awaitTermination(1, TimeUnit.DAYS);
            for (int i = 1; i < nodeCount; i++) {
                assert (finishedKs.contains(i)) : "Potential shortest path(s) of length K=" + i + " not explored!";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Path<CHNValue<T>> chgSP = shortestKPaths.stream().filter(Objects::nonNull).min(Comparator.comparingDouble(Path::getTotalWeight)).orElse(null);

        if (chgSP == null) return null;
        return Path.of(chgSP.getEdges().stream().map(e -> this.getEdge(e.getFrom().getValue().node, e.getTo().getValue().node)));
    }

    public Path<T> shortestConservativePath(T s, T t) {
        //return shortestConservativePath(this.getNode(s), this.getNode(t));
        return shortestConservativePathThreads(this.getNode(s), this.getNode(t));
    }

}
