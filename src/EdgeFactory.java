@FunctionalInterface
public interface EdgeFactory<T, E extends Edge<T>> {
    E create(Node<T> from, Node<T> to, Double weight);
}
