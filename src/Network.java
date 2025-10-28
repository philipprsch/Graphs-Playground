import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Network<T, E extends Edge<T>> extends DirectedGraph<T, E> {

    protected Node<T> s;
    protected Node<T> t;

    private static final Logger LOG = Logger.getLogger(Network.class.getName());

    public Network() {
        super();
        LOG.setLevel(Level.INFO);
    }
    public Network(T s, T t) {
        super();
        this.s = addNode(s);
        this.t = addNode(t);
        LOG.setLevel(Level.INFO);
    }

    public Node<T> getS() {
        return s;
    }

    public void setS(Node<T> s) {
        this.s = s;
    }
    public void setS(T s) {
        this.s = addNode(s);
    }

    public Node<T> getT() {
        return t;
    }

    public void setT(Node<T> t) {
        this.t = t;
    }

    public void setT(T t) {
        this.t = addNode(t);
    }


    public Map<Edge<T>, Double> getZeroFlow() {
        return generateFlow(e -> 0.0);
    }
    public Map<Edge<T>, Double> generateFlow(Function<Edge<T>, Double> generator) {
        Map<Edge<T>, Double> flow = new HashMap<>();
        this.getEdges().forEach((e) -> flow.put(e, generator.apply(e)));
        return flow;
    }
    public double flowValue(Map<Edge<T>, Double> flow) {
        return this.s.getOutgoingEdges().stream().map(flow::get).reduce(Double::sum).orElse(0.0); //0.0 flow for no outgoing edges
    }
    //Below variables may be directly used by functions, taking a maximal flow or max. aug. netw. as input
    //without recalling maximizeFlow() on this network
    private Map<Edge<T>, Double> lastMaxFlow;
    private AugmentingNetwork<T> lastMaxAugmentingNetwork;
    public Set<Node<T>> getMinCutSNodesOfMaxFlow(Map<Edge<T>, Double> flow) {
        AugmentingNetwork<T> augmentingNetwork = AugmentingNetwork.of(this, flow);
        return augmentingNetwork.getReachableNodes(augmentingNetwork.getS()).stream().map(n -> {
            return this.getNode(n.getValue());
        }).collect(Collectors.toSet());
    }
    public Set<Node<T>> getMinCutSNodesOfMaxAug(AugmentingNetwork<T> augmentingNetwork) {
        return augmentingNetwork.getReachableNodes(augmentingNetwork.getS()).stream().map(n -> {
            return this.getNode(n.getValue());
        }).collect(Collectors.toSet());
    }
    public boolean isFlowMaximal(Map<Edge<T>, Double> flow) {
        AugmentingNetwork<T> augmentingNetwork = AugmentingNetwork.of(this, flow);
        Path<T> augmentingPath = augmentingNetwork.shortestPath(augmentingNetwork.getS(), augmentingNetwork.getT());
        return augmentingPath == null;
    }

    public Map<Edge<T>, Double> maximizeFlow(Map<Edge<T>, Double> flow) {
        return maximizeFlow(flow, new HashSet<>());
    }
    public Map<Edge<T>, Double> maximizeFlow() {
        return maximizeFlow(getZeroFlow());
    }


    public Map<Edge<T>, Double> maximizeFlow(Map<Edge<T>, Double> flow, Set<Edge<T>> minAugEdges) {

        if (this.s == null || this.t == null) throw new RuntimeException("Source / Drain undefined");

        AugmentingNetwork<T> augmentingNetwork = AugmentingNetwork.of(this, flow);
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        int iterations = 0;

        while (true) {

            Path<T> augmentingPath = augmentingNetwork.shortestPath(augmentingNetwork.getS(), augmentingNetwork.getT());

            graphs.add(GraphUtils.toGraphviz(this, (e) -> "F: " + flow.get(e).toString(), new GraphUtils.NetworkNodeComparator<T>()));
            graphLabels.add("Net " + iterations);
            graphs.add(GraphUtils.toGraphviz(augmentingNetwork, new GraphUtils.NetworkNodeComparator<T>()));
            graphLabels.add("AugNet " + iterations + "P: " + (augmentingPath != null ? augmentingPath : "None. Done."));

            if (augmentingPath == null) break;
            //Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));
            augmentingNetwork.augmentAlongPath(flow, augmentingPath);

            iterations++;
        }
        lastMaxAugmentingNetwork = augmentingNetwork;
        lastMaxFlow = flow;

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));

        return flow;
    }
    public Map<Edge<T>, Double> getMinimalCapacityIncreasement(double K, Map<Edge<T>, Double> originalFlow) {
        if (this.s == null || this.t == null) throw new RuntimeException("Source / Drain undefined");

        //Create a copy of the current network (including edges), such that this edges capacities are not changed (increased)
        GraphUtils.GraphMapping<T, E, T, Edge<T>, Network<T, Edge<T>>> incrCapNetMapping = GraphUtils.mapGraph(
                this,
                Function.identity(),
                (edge, nodeMapping) -> {
                    return new Edge<T>(nodeMapping.get(edge.getFrom()), nodeMapping.get(edge.getTo()), edge.getWeight());
                },
                Network<T, Edge<T>>::new,
                (newGraph, nodeMapping, edgeMapping) -> {
                    newGraph.setS(nodeMapping.get(this.getS()));
                    newGraph.setT(nodeMapping.get(this.getT()));
                }
        );
        Network<T, Edge<T>> incrCapNet = incrCapNetMapping.getNewGraph();

        Map<Edge<T>, Double> inc = this.getZeroFlow();

        //Create flow variable for incrCapNet Graph (copy flow from originalFlow [defined for this])
        Map<Edge<T>, Double> flow = new HashMap<>(Helpers.joinOnKey(originalFlow, incrCapNetMapping.getEdgeMapping()));
        Set<Edge<T>> blockingEdges = new HashSet<>(); //Set storing edges that (were) limiting flow of value K

        //Debugging stuff
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        int iterations = 0;

        AugmentingNetwork<T> augmentingNetwork = AugmentingNetwork.of(incrCapNet, flow);
        while (true) {
            double currentFlowValue = incrCapNet.flowValue(flow);
            if (currentFlowValue >= K) break;

            Path<T> augmentingPath = augmentingNetwork.shortestPath(augmentingNetwork.getS(), augmentingNetwork.getT());

            graphs.add(GraphUtils.toGraphviz(incrCapNet, (e) -> "F: " + flow.get(e).toString(), new GraphUtils.NetworkNodeComparator<T>()));
            //Graph label is determined by below augmentingPath==null IF statement
            graphs.add(GraphUtils.toGraphviz(augmentingNetwork, new GraphUtils.NetworkNodeComparator<T>()));
            graphLabels.add("AugNet " + iterations + "P: " + (augmentingPath != null ? augmentingPath : "None. Done."));

            if (augmentingPath == null) {
                //Flow is maximal -> find the respective minimal cut -> Set cut-edges weights to Infinity
                Set<Node<T>> minCutNodes = incrCapNet.getMinCutSNodesOfMaxAug(augmentingNetwork);
                Set<Edge<T>> minCutEdges = Graph.neighbourEdges(minCutNodes);
                graphLabels.add("Net " + iterations + "Infinitized: " + minCutEdges.stream().map(e -> e.toString() + ", ").reduce(String::concat).orElse("No min-cut edges."));
                if (minCutEdges.isEmpty()) return null; //Return null (NO SOLUTION) if there is no (was never a) path from S to T in original network
                minCutEdges.forEach(e -> {
                    blockingEdges.add(e);
                    //if (e )
                    e.setWeight(Double.POSITIVE_INFINITY);
                });
                //Update augmenting network after having changed network edges capacities
                augmentingNetwork = AugmentingNetwork.of(incrCapNet, flow);
            } else {
                graphLabels.add("Net " + iterations);
                augmentingNetwork.augmentAlongPath(flow, augmentingPath, Math.min(augmentingPath.getMinWeight(), K - currentFlowValue));
            }

            iterations++;
        }
        //The desired flow K is achieved -> set "infinitized" edges increasement according to their original (this) weights
        //and current flow (of value K)
        Map<E, Edge<T>> incrCapNetNodeMap = incrCapNetMapping.getEdgeMapping();
        Map<Edge<T>, E> thisNodeMap = Helpers.reverseMap(incrCapNetNodeMap);

        blockingEdges.forEach(be -> {
            E originalBE = thisNodeMap.get(be);
            inc.put(originalBE, flow.get(be) - originalBE.getWeight());
        });

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));

        return inc;

    }



}
