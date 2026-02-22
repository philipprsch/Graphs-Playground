public class Edge<T> implements GraphvizComponent {
    protected final Node<T> from;
    protected final Node<T> to;
    protected Double weight;

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
    public void setWeight(Double weight) {
        this.weight = weight;
    }

    //Use clone() to get a new Edge-Instance with all the attributes of this
    //except for new from and to nodes. (Useful for subgraph creation)
    //Override this methods in subclasses of Edge to transfer sub-class specific attributes
    public Edge<T> clone(Node<T> newFrom, Node<T> newTo) {
        return new Edge<>(newFrom, newTo, this.getWeight());
    }

    public Edge<T> reversed() {
        return this.clone(this.getTo(), this.getFrom());
    }
    @Override
    public String toString() {
        return isWeighted()
                ? from + " --(" + weight + ")--> " + to
                : from + " --> " + to;
    }

    @Override
    public String toGraphviz() {
        return isWeighted()
                ? "C: " + weight
                : "null";
    }
}
