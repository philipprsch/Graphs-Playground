import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.HashBiMap;
import org.la4j.*;
import com.google.common.collect.BiMap;
import org.la4j.Vector;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.matrix.functor.MatrixPredicate;

import static org.la4j.Matrices.ZERO_MATRIX;

public abstract class Graph<T, E extends Edge<T>> {

    public Set<Node<T>> getReachableNodes(Node<T> start) {
        if (!nodes.containsKey(start.getValue())) throw new IllegalArgumentException("Node not part of Graph");

        LinkedList<Node<T>> queue = new LinkedList<>();
        Set<Node<T>> visited = new HashSet<>();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node<T> currentNode = queue.pop();
            for (Edge<T> outgoing : currentNode.getOutgoingEdges()) {
                Node<T> neighbour = outgoing.getTo();
                if (visited.add(neighbour)) {
                    queue.add(neighbour);
                }

            }
        }
        return visited;
    }

    protected static class EdgeKey<T> {
        final Node<T> start;
        final Node<T> end;

        EdgeKey(Node<T> start, Node<T> end) {
            this.start = start;
            this.end = end;
        }
        public EdgeKey<T> opposite() {
            return new EdgeKey<>(this.end, this.start);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Graph.EdgeKey)) return false;
            Graph.EdgeKey key = (EdgeKey) o;
            return Objects.equals(start, key.start) &&
                    Objects.equals(end, key.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    //Use extra NodeIndexer Map to ensure consistent ordering in connectivity matrix
    protected final BiMap<Integer, Node<T>> nodeIndexer = HashBiMap.create();

    protected final Map<T, Node<T>> nodes = new HashMap<>();
    protected final Map<EdgeKey<T>, E> edges = new HashMap<>();
    protected final EdgeFactory<T, E> edgeFactory;

    protected Matrix connectivityMatrix = Matrix.zero(0, 0);
    protected boolean isConMatUpToDate = false;

    public Graph() {
        this((from, to, weight) -> (E) new Edge<>(from, to, weight));
    }

    public Graph(EdgeFactory<T, E> edgeFactory) {
        this.edgeFactory = edgeFactory;
    }

    public Node<T> addNode(T value) {
        boolean nodeIsNew = !nodes.containsKey(value);
        Node<T> node = nodes.computeIfAbsent(value, Node::new);
        if (nodeIsNew) { //Only index new nodes
            nodeIndexer.put(nodes.size()-1, node);
            isConMatUpToDate = false;
        }
        return node;
    }

    public void addNodes(Stream<T> nodeValues) {
        nodeValues.forEach(this::addNode);
    }

    public Node<T> getNode(T value) {
        return nodes.get(value);
    }

    public Collection<Node<T>> getNodes() {
        return nodes.values();
    }

    public List<E> getEdges() {
        return List.copyOf(edges.values());
    }

    public final void addExistingEdge(E edge) {
        //Check edge validity
        if (!nodes.containsKey(edge.getFrom().getValue()) || !nodes.containsKey(edge.getTo().getValue())) {
            throw new IllegalArgumentException("Edge Nodes do not exist in this graph");
        }
        if (this.hasEdge(edge)) throw new IllegalArgumentException("Edge "+ edge.toString() +" already exists");
        //Call sub-class specific edge addition code
        _addExistingEdge(edge);
        //Store edge, update non-sub-class-specific graph-structure-depicting variables
        edges.put(new EdgeKey<>(edge.getFrom(), edge.getTo()), edge);
    };

    //Internal methods for Sub-class specific edge adding functionality. Only called in final addExistingEdge
    public abstract void _addExistingEdge(E edge);

    public E addEdge(T from, T to, Double weight) {
        Node<T> fromNode = addNode(from);
        Node<T> toNode = addNode(to);

        E edge = edgeFactory.create(fromNode, toNode, weight);
        addExistingEdge(edge);

        return edge;
    }

    public E addCopyOfEdge(E edge) {
        Node<T> cloneFrom = this.addNode(edge.getFrom().getValue());
        Node<T> cloneTo = this.addNode(edge.getTo().getValue());
        Edge<T> clonedEdge = edge.clone(cloneFrom, cloneTo);
        this.addExistingEdge((E) clonedEdge);
        return (E) clonedEdge;
    }

    public abstract Edge<T> getEdge(Node<T> from, Node<T> to);

    public E addEdge(T from, T to) {
        return addEdge(from, to, null);
    }

    public abstract void removeEdge(E edge);


    //Below methods rely on setting both Outgoing and Incoming edges for both start and end node
    //for undirected graphs
    public static <T> Stream<Node<T>> neighboursStream(Set<Node<T>> S) {
        return S.parallelStream().flatMap(node -> node.getOutgoingEdges().stream().map(Edge::getTo));
    }
    public static <T> Set<Node<T>> neighbours(Set<Node<T>> S) {
        return neighboursStream(S).collect(Collectors.toSet());
    }
    public static <T> Stream<Edge<T>> neighbourEdgesStream(Set<Node<T>> S) {
        return S.stream().flatMap(n -> n.getOutgoingEdges().stream().filter(e -> !S.contains(e.getTo())));
    }
    public static <T> Set<Edge<T>> neighbourEdges(Set<Node<T>> S) {
        return neighbourEdgesStream(S).collect(Collectors.toSet());
    }

    public abstract boolean hasEdge(Node<T> from, Node<T> to);

    public boolean hasEdge(Edge<T> edge) {
        return this.hasEdge(edge.getFrom(), edge.getTo());
    }

    public double getTotalWeight() {
        return this.edges.values().stream().mapToDouble(Edge::getWeight).sum();
    }

    public Matrix getConnectivityMatrix() {
        if (isConMatUpToDate) return connectivityMatrix;
        Matrix mat = new Basic2DMatrix(this.nodes.size(), this.nodes.size());

        // Generate connectivity matrix
        for (int i = 0; i < this.nodes.size(); i++) {
            for (int j = 0; j < this.nodes.size(); j++) {
                mat.set(i, j, (hasEdge(nodeIndexer.get(i), nodeIndexer.get(j)) ? 1 : 0));
            }
        }
        this.connectivityMatrix = mat;
        this.isConMatUpToDate = true;
        return mat;
    }

    //TODO: Implementation of Connectivity Matrix has terrible time complexity of V^3, improve!
    public abstract boolean hasCircle();
}
