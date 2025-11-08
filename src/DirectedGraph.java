import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DirectedGraph<T, E extends Edge<T>> extends Graph<T, E> {

    private static final Logger LOG = Logger.getLogger(DirectedGraph.class.getName());

    public DirectedGraph() {
        super();
        LOG.setLevel(Level.OFF);
    }

    public DirectedGraph(EdgeFactory<T, E> edgeFactory) {
        super(edgeFactory);
    }

    @Override
    public void addExistingEdge(E edge) {
        edges.put(new EdgeKey<>(edge.getFrom(), edge.getTo()), edge);
        edge.getFrom().addOutgoingEdge(edge);
        edge.getTo().addIncomingEdge(edge);
    }

    @Override
    public void removeEdge(E edge) {
        EdgeKey<T> key = new EdgeKey<>(edge.getFrom(), edge.getTo());

        //System.out.println("Before remove: " + edges.get(key).toString() + " Removing: " + edge.toString());
        if (!edges.remove(key, edge)) {
            throw new RuntimeException("Removing edge failed, wrong mapping");
        }
        edge.getFrom().removeOutgoingEdge(edge);
        edge.getTo().removeIncomingEdge(edge);
    }

    //Get the shortest - in terms of summed edge weights - path in a know to be ACYCLIC graph

    private final Map<EdgeKey<T>, Path<T>> ASPMemory = Collections.synchronizedMap(new HashMap<>());


    static class AcyclicShortestPathTask<T> extends RecursiveTask<Path<T>> {
        private final Node<T> startNode;
        private final Node<T> endNode;
        private final int depth;
        private final int seqDepthThreshold;
        private final DirectedGraph<T, Edge<T>> graph;

        public AcyclicShortestPathTask(DirectedGraph<T, Edge<T>> graph, Node<T> startNode, Node<T> endNode, int depth, int seqDepthThreshold) {
            this.startNode = startNode;
            this.endNode = endNode;
            this.depth = depth;
            this.seqDepthThreshold = seqDepthThreshold;
            this.graph = graph;
        }

        @Override
        protected Path<T> compute() {

            if (startNode.equals(endNode)) return Path.emptyFinalWeights();

            EdgeKey<T> pathKey = new EdgeKey<>(startNode, endNode);

            if (graph.ASPMemory.containsKey(pathKey)) {
                return graph.ASPMemory.get(pathKey);
            }

            if (depth > seqDepthThreshold) {
                //NOTE: Sequential Impl. uses and fills the same ASPMemory map
                return graph.acyclicShortestPathSequential(startNode, endNode, depth);
            }

            List<Edge<T>> incomingEdges = new LinkedList<>(endNode.getIncomingEdges());
            Edge<T> currIncomingEdge = incomingEdges.removeFirst(); //Grab one edge to compute one job in current thread

            LinkedList<AcyclicShortestPathTask<T>> aspTasks = new LinkedList<>();
            HashMap<AcyclicShortestPathTask<T>, Edge<T>> taskOrigIncEdgeMap = new HashMap<>();
            for (Edge<T> incoming: incomingEdges) {
                AcyclicShortestPathTask<T> newTask = new AcyclicShortestPathTask<>(graph, startNode, incoming.getFrom(), depth + 1, seqDepthThreshold);
                taskOrigIncEdgeMap.put(newTask, incoming);
                aspTasks.add(newTask); newTask.fork();
            }
            //Compute only one shortest incoming path in current thread
            Path<T> shortestPath = new AcyclicShortestPathTask<>(graph, startNode, currIncomingEdge.getFrom(), depth + 1, seqDepthThreshold).compute().withEdge(currIncomingEdge);

            for (AcyclicShortestPathTask<T> aspTask : aspTasks) {
                Path<T> altPath = aspTask.join().withEdge(taskOrigIncEdgeMap.get(aspTask));
                if (altPath.getTotalWeight() < shortestPath.getTotalWeight()) shortestPath = altPath; //Wait for all shortest paths for each incoming edge to be computed
            }

            graph.ASPMemory.put(pathKey, shortestPath);

            return shortestPath;
        }
        //Sequential implementation of the task outsourced to parent class
    }
    private final static int ASP_MAX_SEQ_PATH_LEN = 40;
    public Path<T> acyclicShortestPathThreads(Node<T> startNode, Node<T> endNode, int maxExpectedPathLength) {
        try (ForkJoinPool pool = new ForkJoinPool()) { // defaults to # of cores
            AcyclicShortestPathTask<T> initialTask = new AcyclicShortestPathTask<>((DirectedGraph<T, Edge<T>>) this, startNode, endNode, 0, maxExpectedPathLength - ASP_MAX_SEQ_PATH_LEN);
            return pool.invoke(initialTask);
        }
    }
    public Path<T> acyclicShortestPathThreads(T s , T t, int maxExpectedPathLength) {
        return acyclicShortestPathThreads(getNode(s), getNode(t), maxExpectedPathLength);
    }

    public Path<T> acyclicShortestPathSequential(Node<T> startNode, Node<T> endNode, int debug_depth) {
        if (!nodes.containsKey(startNode.getValue()) || !nodes.containsKey(endNode.getValue())) throw new IllegalArgumentException("Node not part of Graph");

        if (startNode.equals(endNode)) return Path.empty();

        EdgeKey<T> pathKey = new EdgeKey<>(startNode, endNode);

        //System.out.println("\t".repeat(debug_depth) + "Path: " + startNode + " -> " + endNode + (ASPMemory.containsKey(pathKey) ? " MEMORY" : ""));
        //Use containsKey to return null only if we mapped pathKey to null <=>
        //we have previously computed, that no path from S to T exists
        if (ASPMemory.containsKey(pathKey)) {
            return ASPMemory.get(pathKey);
        }

        LOG.finest("\t".repeat(debug_depth) + "Path: " + startNode + " -> " + endNode);

        List<Path<T>> extIncomingPaths = new LinkedList<>();

        for (Edge<T> incoming: endNode.getIncomingEdges()) {
            Path<T> incomingPath = acyclicShortestPathSequential(startNode, incoming.getFrom(), debug_depth + 1);
            if (incomingPath != null) extIncomingPaths.add(incomingPath.withEdge(incoming));
        }

        Path<T> shortestPath = extIncomingPaths.stream().min(Comparator.comparingDouble(Path::getTotalWeight)).orElse(null);
        ASPMemory.put(pathKey, shortestPath);

        return shortestPath;

    }

    public Path<T> acyclicShortestPathSequential(T startNode, T endNode) {
        return acyclicShortestPathSequential(getNode(startNode), getNode(endNode), 0);
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
    public Path<T> shortestConservativePathNonThreads(Node<T> s, Node<T> t, boolean forkJoin) {

        int nodeCount = this.nodes.size();
        DirectedGraph<CHNValue<T>, Edge<CHNValue<T>>> chg = this.getConservativeHelperGraph();

        //Debugging stuff
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        graphs.add(GraphUtils.toGraphviz(this));
        graphs.add(GraphUtils.toGraphviz(chg));
        graphLabels.add("G"); graphLabels.add("H");

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));

        long globalStart = System.nanoTime();
        List<Path<CHNValue<T>>> shortestKPaths = new LinkedList<>();
        for (int k = 1; k < nodeCount; k++) {
            if (forkJoin) {
                shortestKPaths.add(chg.acyclicShortestPathThreads(new CHNValue<>(s, 0), new CHNValue<>(t, k), k));
            } else {
                shortestKPaths.add(chg.acyclicShortestPathSequential(new CHNValue<>(s, 0), new CHNValue<>(t, k)));
            }
        }
        Path<CHNValue<T>> chgSP = shortestKPaths.stream().filter(Objects::nonNull).min(Comparator.comparingDouble(Path::getTotalWeight)).orElse(null);
        long totalTime = System.nanoTime() - globalStart;
        System.out.println((forkJoin ? "ForkJoin: " : "Sequential: ") + String.format("%6.1f", (totalTime / 1_000_000.0)) + "ms");

        if (chgSP == null) return null;
        return Path.of(chgSP.getEdges().stream().map(e -> this.getEdge(e.getFrom().getValue().node, e.getTo().getValue().node)));
    }

    public Path<T> shortestConservativePathThreads(Node<T> s, Node<T> t) {

        int nodeCount = this.nodes.size();
        DirectedGraph<CHNValue<T>, Edge<CHNValue<T>>> chg = this.getConservativeHelperGraph();

        //Debugging remove later
        chg.acyclicShortestPathSequential(new CHNValue<>(s, 0), new CHNValue<>(t, 4));
        //End

        //Debugging stuff
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        graphs.add(GraphUtils.toGraphviz(this));
        graphs.add(GraphUtils.toGraphviz(chg));
        graphLabels.add("G"); graphLabels.add("H");

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));

        Function<Integer, Double> loadFunction = k -> {
            return (double) (k + 1) * (this.getNodes().size() + this.getEdges().size()); //Does not produce equal time for all threads!
            //return (double) (k + 1) * Math.pow(this.getNodes().size(), 2);
            //return Math.pow(k, 3) * 1000.0;
        };

        Set<Path<CHNValue<T>>> shortestKPaths = Collections.synchronizedSet(new HashSet<>());
        Set<Integer> finishedKs = Collections.synchronizedSet(new HashSet<>());
        int p = Runtime.getRuntime().availableProcessors();
        //LOG.fine("Starting " + (nodeCount -1) + " k-path finding jobs on " + p + " processors");
        System.out.println("Starting " + (nodeCount -1) + " k-path finding jobs on " + p + " processors");
        try (ExecutorService exec = Executors.newFixedThreadPool(p)) {

            LoadDistributor<Integer> ld = LoadDistributor.moduloBased();
            Map<Integer, Set<Integer>> threadToJobsMap = ld.apply(IntStream.range(1, nodeCount).boxed().collect(Collectors.toList()), p);

            ThreadJobCollection<Integer> threadJobsInfo =
                    new ThreadJobCollection<>(threadToJobsMap, loadFunction, "Load strategy: " + ld);

            int threadIDCounter = 0;

            long globalStart = System.nanoTime();
            for (Set<Integer> pidJobs : threadToJobsMap.values()) { //(bounds.length - 1) = Math.min(p, nodeCount -1);
                final int threadIDCounterCurrent = threadIDCounter++;
                exec.submit(() -> {

                    long start = System.nanoTime();

                    for (Integer k : pidJobs) {
                        shortestKPaths.add(chg.acyclicShortestPathSequential(new CHNValue<>(s, 0), new CHNValue<>(t, k)));
                        finishedKs.add(k);
                    }

                    long duration = System.nanoTime() - start;

                    threadJobsInfo.setThreadName(threadIDCounterCurrent, Thread.currentThread().getName());
                    threadJobsInfo.setThreadTime(threadIDCounterCurrent, duration);
                });
            }
            exec.shutdown();
            //noinspection ResultOfMethodCallIgnored
            exec.awaitTermination(1, TimeUnit.DAYS);
            for (int i = 1; i < nodeCount; i++) {
                assert (finishedKs.contains(i)) : "Potential shortest path(s) of length K=" + i + " not explored!";
            }

            threadJobsInfo.setTotalTime(System.nanoTime() - globalStart);
            System.out.println(threadJobsInfo);

        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception during ExecutorService");
        }
        Path<CHNValue<T>> chgSP = shortestKPaths.stream().filter(Objects::nonNull).min(Comparator.comparingDouble(Path::getTotalWeight)).orElse(null);

        if (chgSP == null) return null;
        return Path.of(chgSP.getEdges().stream().map(e -> this.getEdge(e.getFrom().getValue().node, e.getTo().getValue().node)));
    }

    public enum ExecutionStrategy {
        Sequential(),
        ExecutorService(),
        ForkJoinPool();

        ExecutionStrategy() {}
    }

    public Path<T> shortestConservativePath(Node<T> s, Node<T> t, ExecutionStrategy strategy) {
        //return shortestConservativePath(this.getNode(s), this.getNode(t));
        return switch (strategy) {
            case Sequential -> shortestConservativePathNonThreads(s, t, false);
            case ForkJoinPool -> shortestConservativePathNonThreads(s, t, true);
            case ExecutorService -> shortestConservativePathThreads(s, t);
            case null -> throw new IllegalArgumentException("Invalid execution strategy");
        };
    }
    public Path<T> shortestConservativePath(T s, T t, ExecutionStrategy strategy) {
        //return shortestConservativePath(this.getNode(s), this.getNode(t));
        return shortestConservativePath(this.getNode(s), this.getNode(t), strategy);
    }




}
