import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Path<T> {
    private final LinkedList<Edge<T>> edges = new LinkedList<>();

    private Double totalWeight = null;

    //weightUnmod : boolean : set to false by user if edges of this path may be modified
    private final boolean edgeWeightsFinal;

    public Path(boolean edgeWeightsFinal) {
        this.edgeWeightsFinal = edgeWeightsFinal;
    }

    public Path() {
        this(false);
    }

    // Copy constructor (useful for building new paths from existing ones)
    public Path(Path<T> other) {
        this(other, other.edgeWeightsFinal);
    }
    public Path(Path<T> other, boolean edgeWeightsFinal) {
        this.edges.addAll(other.edges);
        this.edgeWeightsFinal = edgeWeightsFinal;
    }

    public static <U> Path<U> empty() {
        return new Path<>();
    }
    public static <U> Path<U> emptyFinalWeights() {
        return new Path<>(true);
    }

    public static <T> Path<T> of(Stream<Edge<T>> edges) {
        return Path.of(edges, false);
    }
    public static <T> Path<T> of(Stream<Edge<T>> edges, boolean weightUnmod) {
        Path<T> newPath = new Path<>(weightUnmod);
        edges.forEach(newPath::addEdge);
        return newPath;
    }

    public Path<T> withEdge(Edge<T> edge) {
        Path<T> newPath = new Path<>(this);
        newPath.addEdge(edge);
        return newPath;
    }

    public void addEdge(Edge<T> edge) {
        edges.add(edge);
        if (this.totalWeight != null) this.totalWeight += edge.getWeight();
    }

    public List<Edge<T>> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Node<T> getStartNode() {
        return edges.isEmpty() ? null : edges.getFirst().getFrom();
    }

    public Node<T> getEndNode() {
        return edges.isEmpty() ? null : edges.getLast().getTo();
    }

    public double getTotalWeight(boolean useCached) {
        if (useCached && this.totalWeight != null) return this.totalWeight;
        double sum = 0.0;
        for (Edge<T> edge : edges) {
            sum += edge.getWeight() != null ? edge.getWeight() : 0.0;
        }
        totalWeight = sum;
        return sum;
    }
    public double getTotalWeight() {
        return getTotalWeight(edgeWeightsFinal);
    }
    public double getMinWeight() {
        Edge<T> minEdge = this.getEdges().stream().min(Comparator.comparingDouble(Edge::getWeight)).orElseThrow();
        return minEdge.getWeight();
    }

    @Override
    public String toString() {
        if (edges.isEmpty()) return "[empty path]";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < edges.size(); i++) {
            Edge<T> e = edges.get(i);
            sb.append(e.getFrom());
            if (e.isWeighted()) {
                sb.append(" --(").append(e.getWeight()).append(")--> ");
            } else {
                sb.append(" --> ");
            }
            if (i == edges.size() - 1) {
                sb.append(e.getTo());
            }
        }
        sb.append("  [total=").append(getTotalWeight()).append("]");
        return sb.toString();
    }
}
