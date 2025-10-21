import java.util.*;
import java.util.function.Function;

public class Network<T, E extends Edge<T>> extends DirectedGraph<T, E> {

    private Node<T> s;
    private Node<T> t;

    public Network() {
        super();
    }
    public Network(T s, T t) {
        super();
        this.s = addNode(s);
        this.t = addNode(t);
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
    public Network<T, AugmentingEdge<T>> getAugmentingNetwork(Map<Edge<T>, Double> flow) {
        GraphUtils.GraphMapping<T, E, T, AugmentingEdge<T>, Network<T, AugmentingEdge<T>>> graphMapping = GraphUtils.mapGraph(
                this,
                Function.identity(),
                (edge, nodeMapping) -> {
                    double newWeight = edge.getWeight() - flow.get(edge);
                    return (newWeight > 0 ? new AugmentingEdge<T>(nodeMapping.get(edge.getFrom()), nodeMapping.get(edge.getTo()), newWeight, edge, false) : null);
                },
                Network<T, AugmentingEdge<T>>::new,
                (newGraph, nodeMapping, edgeMapping) -> { //Map source and drain points
                    newGraph.setS(nodeMapping.get(this.s));
                    newGraph.setT(nodeMapping.get(this.t));
                });

        Network<T, AugmentingEdge<T>> augmentingNetwork = graphMapping.getNewGraph();

        Collection<AugmentingEdge<T>> augNetNewEdges = new LinkedList<>();
        for (AugmentingEdge<T> edge : augmentingNetwork.getEdges()) {
            Double edgeFlow = flow.get(edge.getOriginalEdge());
            if (edgeFlow > 0) {
                augNetNewEdges.add(new AugmentingEdge<>(
                        edge.getTo(),
                        edge.getFrom(),
                        edgeFlow,
                        edge.getOriginalEdge(),
                        true));
            }
        }
        for (AugmentingEdge<T> edge : augNetNewEdges) {
            augmentingNetwork.addExistingEdge(edge);
        }
        return augmentingNetwork;
    }
    private Edge<T> augmentAlongPath(Map<Edge<T>, Double> flow, Path<T> augmentingPath) {
        Edge<T> minEdge = augmentingPath.getEdges().stream().min(Comparator.comparingDouble(Edge::getWeight)).orElseThrow();
        double gamma = minEdge.getWeight();
        System.out.println("Augmenting along Path: " + augmentingPath.toString() + " with g=" + gamma);
        augmentingPath.getEdges().forEach(e -> {
            AugmentingEdge<T> augEdge = (AugmentingEdge<T>) e;
            assert flow.containsKey(augEdge.getOriginalEdge()) : "Tried to augment non-flow-reg. Edge: " + augEdge.getOriginalEdge().toString() + " of AugEdge: " + augEdge.toString();
            Double newFlow = flow.compute(augEdge.getOriginalEdge(), (k, v) -> {
                return (augEdge.isBackFlow() ? v - gamma : v + gamma);
            });
            //Update the Augmentationnetwork according to the new flow assigned to augEdge.getOriginalEdge()
            if (!augEdge.update(newFlow)) {
                System.out.println("Must remove path edge: " + augEdge);
                this.removeEdge((E) augEdge);
            }
            AugmentingEdge<T> counterEdge = (AugmentingEdge<T>) getEdge(augEdge.getTo(), augEdge.getFrom());
            if (counterEdge != null) {
                System.out.println("Counter edge exists and is: " + counterEdge.toString());
                assert counterEdge.isBackFlow() != augEdge.isBackFlow() : "Counter edge is not backflow";
                //If counter Edge already is in Graph just update it and delete if no longer needed.
                if (!counterEdge.update(newFlow)) {
                    System.out.println("Must remove counter edge");
                    this.removeEdge((E) counterEdge);
                } else {
                    System.out.println("Updated counter edge: " + counterEdge.toString());
                }
            } else {
                System.out.println("Counter edge does not exist");
                //Experimentally create counter Edge and only add it to Graph if required by flow.
                AugmentingEdge<T> newCounterEdge = new AugmentingEdge<>(augEdge.getTo(), augEdge.getFrom(), 0.0, augEdge.getOriginalEdge(), !augEdge.isBackFlow());
                if (newCounterEdge.update(newFlow)) {
                    System.out.println("Initialized counter edge: " + newCounterEdge.toString());
                    this.addExistingEdge((E) newCounterEdge);
                } else {
                    System.out.println("Did not add counter Edge");
                }
            }

        });
        return minEdge;
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
        return this.s.getOutgoingEdges().stream().map(flow::get).reduce(Double::sum).orElseThrow();
    }


    public Map<Edge<T>, Double> maximizeFlow(Map<Edge<T>, Double> flow) {
        return maximizeFlow(flow, new HashSet<>());
    }
    public Map<Edge<T>, Double> maximizeFlow() {
        return maximizeFlow(getZeroFlow());
    }

    public Map<Edge<T>, Double> maximizeFlow(Map<Edge<T>, Double> flow, Set<Edge<T>> minAugEdges) {

        if (this.s == null || this.t == null) throw new RuntimeException("Source / Drain undefined");

        Network<T, AugmentingEdge<T>> augmentingNetwork = this.getAugmentingNetwork(flow);
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
            Edge<T> minEdge = augmentingNetwork.augmentAlongPath(flow, augmentingPath);
            minAugEdges.add(minEdge);

            iterations++;
        }
        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));

        return flow;
    }
    public Map<Edge<T>, Double> getMinimalCapacityIncreasement(double K, Map<Edge<T>, Double> originalFlow) {
        if (this.s == null || this.t == null) throw new RuntimeException("Source / Drain undefined");

        Map<Edge<T>, Double> inc = getZeroFlow();
        Map<Edge<T>, Double> flow = new HashMap<>(originalFlow);

        Network<T, AugmentingEdge<T>> augmentingNetwork = this.getAugmentingNetwork(originalFlow);
        List<String> graphs = new LinkedList<>();
        List<String> graphLabels = new LinkedList<>();
        int iterations = 0;

        while (true) {
            Path<T> augmentingPath = augmentingNetwork.shortestPath(augmentingNetwork.getS(), augmentingNetwork.getT());

            graphs.add(GraphUtils.toGraphviz(this, (e) -> "F: " + flow.get(e).toString(), new GraphUtils.NetworkNodeComparator<T>()));
            graphLabels.add("Net " + iterations);
            graphs.add(GraphUtils.toGraphviz(augmentingNetwork, new GraphUtils.NetworkNodeComparator<T>()));
            graphLabels.add("AugNet " + iterations + "P: " + (augmentingPath != null ? augmentingPath : "None. Done."));

            if (augmentingPath == null) {
                //Flow is maximal -> find the respective minimal cut -> Set cut-edges weights to Infinity
                break; //Remove this later
            }

            augmentingNetwork.augmentAlongPath(flow, augmentingPath);

            iterations++;

        }

        Helpers.copyToClipboard(GraphUtils.combineIntoClusteredGraph(graphs, graphLabels));

        return inc;

    }



}
