import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Path<T> {
    private final LinkedList<Edge<T>> edges = new LinkedList<>();

    public Path() {}

    // Copy constructor (useful for building new paths from existing ones)
    public Path(Path<T> other) {
        this.edges.addAll(other.edges);
    }

    public static <U> Path<U> empty() {
        return new Path<>();
    }
    public Path<T> withEdge(Edge<T> edge) {
        Path<T> newPath = new Path<>(this);
        newPath.addEdge(edge);
        return newPath;
    }

    public void addEdge(Edge<T> edge) {
        edges.add(edge);
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

    public double getTotalWeight() {
        double sum = 0.0;
        for (Edge<T> edge : edges) {
            sum += edge.getWeight() != null ? edge.getWeight() : 0.0;
        }
        return sum;
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
