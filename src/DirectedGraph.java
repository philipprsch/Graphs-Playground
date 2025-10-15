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

    public Path<T> acyclicShortestPath(Node<T> startNode, Node<T> endNode) {
        if (!nodes.containsKey(startNode.getValue()) || !nodes.containsKey(endNode.getValue())) throw new IllegalArgumentException("Node not part of Graph");

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

}
