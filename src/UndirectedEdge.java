public class UndirectedEdge<T> extends Edge<T> implements GraphvizComponent {

    public UndirectedEdge(Node<T> from, Node<T> to, Double weight) {
        super(from, to, weight);
    }

    public UndirectedEdge(Node<T> from, Node<T> to) {
        super(from, to);
    }
    public Node<T> getA() {
        return from;
    }
    public Node<T> getB() {
        return to;
    }

    @Override
    public Edge<T> clone(Node<T> newFrom, Node<T> newTo) {
        return new UndirectedEdge<>(newFrom, newTo, this.getWeight());
    }

    @Override
    public String toString() {
        return isWeighted()
                ? from + " <--(" + weight + ")--> " + to
                : from + " <--> " + to;
    }

    //Extend equality notion for undirected Edges, such that
    //when calling an edge is a list can be found through edge.reversed()
    //Important for removeIncoming/OutgoingEdge in graph.removeEdge(edge)

    //NOTE: Two undirected edges connecting the same nodes still have different hash values
    //consistent storing / retrieving of undirected edges in HashMaps is outsourced to
    //the wrappers of the maps (e.g. see: getCorrectKey for graph.edges map)
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UndirectedEdge<?>)) return false;
        UndirectedEdge<T> ue = (UndirectedEdge<T>) o;
        return (ue.getFrom() == this.from && ue.getTo()  == this.to) ||
                (ue.getFrom() == this.to && ue.getTo()  == this.from);
    }

}
