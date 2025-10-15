public class Main {
    public static void main(String[] args) {



        DirectedGraph<String, Edge<String>> dag = new DirectedGraph<>();

        // Acyclic, weighted edges
        dag.addEdge("A", "B", 3.0);
        dag.addEdge("A", "C", 1.0);
        dag.addEdge("B", "D", 2.0);
        dag.addEdge("B", "E", 4.0);
        dag.addEdge("B", "F", 6.0);
        dag.addEdge("C", "E", 2.0);
        dag.addEdge("C", "F", 3.0);
        dag.addEdge("D", "G", 2.0);
        dag.addEdge("E", "I", 2.0);
        dag.addEdge("F", "I", 3.0);
        dag.addEdge("G", "I", 1.0);

        Path<String> sp = dag.acyclicShortestPath("A", "A");

        System.out.println(sp.toString());
        System.out.println(sp.getTotalWeight());

        Path<String> newsp = dag.shortestPath("A", "I");

        System.out.println(newsp.toString());
        System.out.println(newsp.getTotalWeight());
    }
}
