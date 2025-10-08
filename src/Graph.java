import java.util.*;
import java.util.function.Function;

public abstract class Graph<T, E extends Edge<T>> {
    protected final Map<T, Node<T>> nodes = new HashMap<>();
    protected final List<E> edges = new ArrayList<>();
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
        return Collections.unmodifiableList(edges);
    }

    public abstract void addExistingEdge(E edge);

    public E addEdge(T from, T to, Double weight) {
        Node<T> fromNode = addNode(from);
        Node<T> toNode = addNode(to);

        E edge = edgeFactory.create(fromNode, toNode, weight);
        addExistingEdge(edge);

        return edge;
    }

    public E addEdge(T from, T to) {
        return addEdge(from, to, null);
    }
}
