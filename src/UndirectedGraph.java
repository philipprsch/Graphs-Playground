import org.la4j.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.la4j.Matrices.ZERO_MATRIX;

public class UndirectedGraph<T, E extends UndirectedEdge<T>> extends Graph<T, E> {

    public UndirectedGraph() {
        this((from, to, weight) -> (E) new UndirectedEdge<>(from, to, weight));
    }

    public UndirectedGraph(EdgeFactory<T, E> edgeFactory) {
        super(edgeFactory);
    }

    @Override
    public void _addExistingEdge(E edge) {
        // For undirected graphs, both nodes share the edge as incoming/outgoing
        Edge<T> reversedEdge = edge.reversed();
        //Use reversedEdge, such that when calling A.getOutgoingEdges() it is
        //guaranteed that edge.getTo() will return node B, and not the same node A
        //eliminating the need of distinct implementations of methods like getReachableNodes()
        //for UndirectedGraph and DirectedGraph
        edge.getFrom().addOutgoingEdge(edge);
        edge.getFrom().addIncomingEdge(reversedEdge);
        edge.getTo().addOutgoingEdge(reversedEdge);
        edge.getTo().addIncomingEdge(edge);

        int fromIndex = this.nodeIndexer.inverse().get(edge.getFrom());
        int toIndex = this.nodeIndexer.inverse().get(edge.getTo());

        //Update connectivity matrix only if it models all the nodes - prevent IndexOutOfBounds exception
        if (this.isConMatUpToDate) {
            this.connectivityMatrix.set(fromIndex, toIndex, 1); //Do not use getter!
            this.connectivityMatrix.set(toIndex, fromIndex, 1);
        }
    }
    //Ensure that Edges (u, w) and (w, u) use the same EdgeKey in the edges map, such that
    //an edge can be found regardless of the specification order of the nodes it connects
    // => no need to overload the edges map with duplicates (two edgeKeys for each edge)
    private EdgeKey<T> getCorrectKey(Node<T> from, Node<T> to) {
        EdgeKey<T> key = new EdgeKey<>(from, to);
        if (edges.containsKey(key)) {
            return key;
        }
        return key.opposite();
    }
    @Override
    public void removeEdge(E edge) {
        EdgeKey<T> key = this.getCorrectKey(edge.getFrom(), edge.getTo());

        if (!edges.remove(key, edge)) {
            throw new RuntimeException("Removing edge failed, wrong mapping");
        }

        edge.getFrom().removeOutgoingEdge(edge);
        edge.getTo().removeIncomingEdge(edge);
        edge.getTo().removeOutgoingEdge(edge);
        edge.getFrom().removeIncomingEdge(edge);

        int fromIndex = this.nodeIndexer.inverse().get(edge.getFrom());
        int toIndex = this.nodeIndexer.inverse().get(edge.getTo());
        this.getConnectivityMatrix().set(fromIndex, toIndex, 0);
        this.getConnectivityMatrix().set(toIndex, fromIndex, 0);
    }
    public Edge<T> getEdge(Node<T> from, Node<T> to) {
        return edges.get(this.getCorrectKey(from, to));
    }
    @Override
    public boolean hasEdge(Node<T> from, Node<T> to) {
        return edges.containsKey(new EdgeKey<>(from, to)) || edges.containsKey(new EdgeKey<>(to, from));
    }
    @Override
    public boolean hasCircle() {
        HashSet<Node<T>> visited = new HashSet<>();
        HashMap<EdgeKey<T>, Edge<T>> usedEdges = new HashMap<>();
        //Have not found any con. mat. based approach for undirected G -> Use BFS
        for (Node<T> node : nodes.values()) {
            if (!visited.contains(node)) { //Perform BFS on nodes only from unexplored ZHK
                LinkedList<Node<T>> queue = new LinkedList<>();

                visited.add(node);
                queue.add(node);

                while (!queue.isEmpty()) {
                    Node<T> currentNode = queue.pop();
                    for (Edge<T> outgoing : currentNode.getOutgoingEdges()) {
                        if (usedEdges.containsKey(new EdgeKey<>(outgoing.getFrom(), outgoing.getTo())) || usedEdges.containsKey(new EdgeKey<>(outgoing.getTo(), outgoing.getFrom()))) {
                            continue;
                        }
                        usedEdges.put(new EdgeKey<>(outgoing.getFrom(), outgoing.getTo()), outgoing);
                        //NOTE: Implementation of addExistingEdge (in UndirectedGraph) ensures getOutgoingEdges
                        //returns only (reversed) edges whose getTo() return the counterpart node to currentNode
                        Node<T> neighbour = outgoing.getTo();
                        if (visited.add(neighbour)) {
                            queue.add(neighbour);
                        } else {
                            return true;
                        }

                    }
                }
            }
        }
        return false;
    }

    //TODO: Extract information on ZHK Count from below MST methods
    public UndirectedGraph<T, UndirectedEdge<T>> greedyMST() {
        UndirectedGraph<T, UndirectedEdge<T>> minForest = new UndirectedGraph<>();
        minForest.addNodes(this.nodes.keySet().stream());
        List<E> sortedEdges = this.edges.values().stream().sorted(Comparator.comparingDouble(Edge::getWeight)).collect(Collectors.toList());
        while (!sortedEdges.isEmpty()) {
            UndirectedEdge<T> nextEdge = sortedEdges.removeFirst();

            UndirectedEdge<T> clonedEdge = minForest.addCopyOfEdge(nextEdge);

            if (minForest.hasCircle()) {
                minForest.removeEdge(clonedEdge);
            }

        }
        return minForest;
    }

    public UndirectedGraph<T, UndirectedEdge<T>> boruvkasMST() {
        UndirectedGraph<T, UndirectedEdge<T>> minForest = new UndirectedGraph<>();
        minForest.addNodes(this.getNodes().stream().map(Node::getValue)); //Copy all nodes
        List<Set<Node<T>>> subTreeNodeSets = this.getNodes().stream().map(n -> new HashSet<>(Set.of(n))).collect(Collectors.toList());
        AtomicInteger concatenationCount = new AtomicInteger();
        while (subTreeNodeSets.size() > 1) {
            concatenationCount.set(0);
            List<UndirectedEdge<T>> minEdgesStream = subTreeNodeSets.parallelStream()
                    .map(subTree -> (UndirectedEdge<T>) Graph.neighbourEdgesStream(subTree)
                            .min(Comparator.comparingDouble(Edge::getWeight))
                            .orElse(null))
                    .filter(Objects::nonNull).toList(); //Remove all the sub-trees with no outgoing edges

            minEdgesStream.forEach(e -> {
                //Pick the (previously found) minimal edge, connecting two sub-trees with each other / one sub-tree to the "outside"
                Node<T> aSub = minForest.getNode(e.getA().getValue());
                Node<T> bSub = minForest.getNode(e.getB().getValue());
                UndirectedEdge<T> clonedEdge = (UndirectedEdge<T>) e.clone(aSub, bSub);
                assert subTreeNodeSets.stream().filter(tr -> tr.contains(e.getA())).count() == 1;
                assert subTreeNodeSets.stream().filter(tr -> tr.contains(e.getB())).count() == 1;
                Set<Node<T>> nodeSetOfA = subTreeNodeSets.stream().filter(tr -> tr.contains(e.getA())).findAny().orElseThrow();
                Set<Node<T>> nodeSetOfB = subTreeNodeSets.stream().filter(tr -> tr.contains(e.getB())).findAny().orElseThrow();
                //Probably: In a previous iteration another edge of same weight as e was picked as the min. edge connecting the previous
                //nodeSets (sub-trees) belonging to A and B, skip this edge
                if (nodeSetOfA == nodeSetOfB) return; //nodeSetOfA == nodeSetOfB <=> minForest.hasEdge(e)
                minForest.addExistingEdge(clonedEdge);
                //assert Collections.disjoint(nodeSetOfA, nodeSetOfB);
                //Merge the node sets / sub-trees into one bigger tree
                nodeSetOfA.addAll(nodeSetOfB);
                subTreeNodeSets.remove(nodeSetOfB);
                concatenationCount.getAndIncrement();
            });
            //The remaining sub-trees can not be merged into a bigger tree / this is not coherent
            if (concatenationCount.get() == 0) break;
        }
        return minForest;
    }

    public static void main(String[] args) {
        Node<String> a = new Node<>("a");
        Node<String> b = new Node<>("b");
        System.out.println(a.hashCode());
        System.out.println(b.hashCode());

        UndirectedGraph<String, UndirectedEdge<String>> undirectedGraph = new UndirectedGraph<>();
        undirectedGraph.addExistingEdge(new UndirectedEdge<>(a,b));
        undirectedGraph.addExistingEdge(new UndirectedEdge<>(b, a));
        System.out.println(undirectedGraph.edges.entrySet().toString());
    }

}
