public class Edge<T> {
    private final Node<T> from;
    private final Node<T> to;
    private final Double weight;

    public Edge(Node<T> from, Node<T> to, Double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public Edge(Node<T> from, Node<T> to) {
        this(from, to, null);
    }

    public Node<T> getFrom() {
        return from;
    }

    public Node<T> getTo() {
        return to;
    }

    public boolean isWeighted() {
        return weight != null;
    }

    public Double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return isWeighted()
                ? from + " --(" + weight + ")--> " + to
                : from + " --> " + to;
    }
}
