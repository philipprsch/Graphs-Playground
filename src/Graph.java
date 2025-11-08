import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public abstract class Graph<T, E extends Edge<T>> {

    static class EdgeKey<T> {
        final Node<T> start;
        final Node<T> end;

        EdgeKey(Node<T> start, Node<T> end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Graph.EdgeKey)) return false;
            Graph.EdgeKey key = (EdgeKey) o;
            return Objects.equals(start, key.start) &&
                    Objects.equals(end, key.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    protected final Map<T, Node<T>> nodes = new HashMap<>();
    protected final Map<EdgeKey, E> edges = new HashMap<>();
    protected final EdgeFactory<T, E> edgeFactory;

    public Graph() {
        this((from, to, weight) -> (E) new Edge<>(from, to, weight));
    }

    public Graph(EdgeFactory<T, E> edgeFactory) {
        this.edgeFactory = edgeFactory;
    }

    public Node<T> addNode(T value) {
        return nodes.computeIfAbsent(value, Node::new);
    }

    public Node<T> getNode(T value) {
        return nodes.get(value);
    }

    public Collection<Node<T>> getNodes() {
        return nodes.values();
    }

    public List<E> getEdges() {
        return List.copyOf(edges.values());
    }

    public abstract void addExistingEdge(E edge);

    public E addEdge(T from, T to, Double weight) {
        Node<T> fromNode = addNode(from);
        Node<T> toNode = addNode(to);

        E edge = edgeFactory.create(fromNode, toNode, weight);
        addExistingEdge(edge);

        return edge;
    }
    public Edge<T> getEdge(Node<T> from, Node<T> to) {
        return edges.get(new EdgeKey<>(from, to));
    }

    public E addEdge(T from, T to) {
        return addEdge(from, to, null);
    }

    public abstract void removeEdge(E edge);


    //Below methods rely on setting both Outgoing and Incoming edges for both start and end node
    //for undirected graphs
    public static <T> Set<Node<T>> neighbours(Set<Node<T>> S) {
        return S.stream().flatMap(node -> {
            return node.getOutgoingEdges().stream().map(Edge::getTo);
        }).collect(Collectors.toSet());
    }
    public static <T> Set<Edge<T>> neighbourEdges(Set<Node<T>> S) {
        return S.stream().flatMap(n -> {
            return n.getOutgoingEdges().stream().filter(e -> {
                return  !S.contains(e.getTo());
            });
        }).collect(Collectors.toSet());
    }

    public boolean hasEdge(Node<T> from, Node<T> to) {
        return edges.containsKey(new EdgeKey<>(from, to));
    }
}
