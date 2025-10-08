public class UndirectedGraph<T, E extends Edge<T>> extends Graph<T, E> {

    public UndirectedGraph() {
        super();
    }

    public UndirectedGraph(EdgeFactory<T, E> edgeFactory) {
        super(edgeFactory);
    }

    @Override
    public void addExistingEdge(E edge) {
        edges.add(edge);

        // For undirected graphs, both nodes share the edge as incoming/outgoing
        edge.getFrom().addOutgoingEdge(edge);
        edge.getFrom().addIncomingEdge(edge);
        edge.getTo().addOutgoingEdge(edge);
        edge.getTo().addIncomingEdge(edge);
    }

}
