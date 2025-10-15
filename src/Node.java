import java.util.*;

public class Node<T> {
    private final T value;
    private final List<Edge<T>> outgoingEdges = new ArrayList<>();
    private final List<Edge<T>> incomingEdges = new ArrayList<>();

    public Node(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public List<Edge<T>> getOutgoingEdges() {
        return Collections.unmodifiableList(outgoingEdges);
    }

    public List<Edge<T>> getIncomingEdges() {
        return Collections.unmodifiableList(incomingEdges);
    }

    void addOutgoingEdge(Edge<T> edge) {
        outgoingEdges.add(edge);
    }
    void removeOutgoingEdge(Edge<T> edge) {
        outgoingEdges.remove(edge);
    }

    void addIncomingEdge(Edge<T> edge) {
        incomingEdges.add(edge);
    }
    void removeIncomingEdge(Edge<T> edge) {
        incomingEdges.remove(edge);
    }
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
