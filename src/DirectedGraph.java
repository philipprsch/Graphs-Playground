import java.util.*;
import java.util.function.Function;

public class DirectedGraph<T, E extends Edge<T>> extends Graph<T, E> {

    public DirectedGraph() {
        super();
    }

    public DirectedGraph(EdgeFactory<T, E> edgeFactory) {
        super(edgeFactory);
    }

    @Override
    public void addExistingEdge(E edge) {
        edges.add(edge);
        edge.getFrom().addOutgoingEdge(edge);
        edge.getTo().addIncomingEdge(edge);
    }

    public Path<T> acyclicShortestPath(Node<T> startNode, Node<T> endNode) {
        if (startNode.equals(endNode)) return Path.empty();

        //if (endNode.getIncomingEdges().isEmpty()) return null;

        List<Path<T>> extIncomingPaths = new LinkedList<>();

        for (Edge<T> incoming: endNode.getIncomingEdges()) {
            Path<T> incomingPath = acyclicShortestPath(startNode, incoming.getFrom());
            if (incomingPath != null) extIncomingPaths.add(incomingPath.withEdge(incoming));
        }

        Optional<Path<T>> shortestPath = extIncomingPaths.stream().min(Comparator.comparingDouble(Path::getTotalWeight));

        return shortestPath.orElse(null);

    }
    public Path<T> acyclicShortestPath(T startNode, T endNode) {
        return acyclicShortestPath(getNode(startNode), getNode(endNode));
    }

    public Path<T> shortestPath(Node<T> startNode, Node<T> endNode) {
        LinkedList<Node<T>> queue = new LinkedList<>();
        HashMap<Node<T>, Path<T>> shortestPath = new HashMap<>();

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


    public DirectedGraph<T, Edge<T>> getAugmentingNetwork(Map<Edge<T>, Double> flow) {
        DirectedGraph<T, Edge<T>> augmentingNetwork = GraphUtils.mapGraph(
                this,
                Function.identity(),
                (edge, nodeMapping) -> {
                    double newWeight = edge.getWeight() - flow.get(edge);
                    return (newWeight > 0 ? new AugmentingEdge<>(nodeMapping.get(edge.getFrom()), nodeMapping.get(edge.getTo()), newWeight, edge, false) : null);
                },
                DirectedGraph::new);

        for (Edge<T> edge : augmentingNetwork.getEdges()) {
            AugmentingEdge<T> augEdge = ((AugmentingEdge<T>) edge);
            Double edgeFlow = flow.get(augEdge.getOriginalEdge());
            if (edgeFlow > 0) {
                augmentingNetwork.addExistingEdge(new AugmentingEdge<>(
                        edge.getTo(),
                        edge.getFrom(),
                        edgeFlow,
                        augEdge.getOriginalEdge(),
                        true));
            }
        }
        return augmentingNetwork;
    }
    private void augmentAlongPath(Map<Edge<T>, Double> flow, Path<T> augmentingPath) {

    }

    public Map<Edge<T>, Double> getZeroFlow() {
        Map<Edge<T>, Double> flow = new HashMap<>();
        this.getEdges().forEach((e) -> flow.put(e, 0.0));
        return flow;
    }

    public void maximizeFlow(Map<Edge<T>, Double> flow, Node<T> s, Node<T> t) {
        Path<T> augmentingPath = shortestPath(s, t);
        while (augmentingPath != null) {

        }
    }



}
