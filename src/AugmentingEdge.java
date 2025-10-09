public class AugmentingEdge<T> extends Edge<T> implements GraphvizComponent {
    private Edge<T> originalEdge;

    private Boolean isBackFlow;

    public AugmentingEdge(Node<T> from, Node<T> to, Double weight, Edge<T> originalEdge, Boolean isBackFlow) {
        super(from, to, weight);
        this.originalEdge = originalEdge;
        this.isBackFlow = isBackFlow;
    }

    public Edge<T> getOriginalEdge() {
        return originalEdge;
    }

    public void setOriginalEdge(Edge<T> originalEdge) {
        this.originalEdge = originalEdge;
    }

    public Boolean isBackFlow() {
        return isBackFlow;
    }

    public void setBackFlow(Boolean backFlow) {
        isBackFlow = backFlow;
    }

    @Override
    public String toGraphviz() {
        return isWeighted()
                ? "Cx: " + this.getWeight()
                : "null";
    }

}
