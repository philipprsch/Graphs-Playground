import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Main {
    public static void main(String[] args) {

        ForkJoinPool pool = new ForkJoinPool();
        int result = pool.invoke(new Main.AcyclicShortestPathTask(5));
        System.out.println("Result: " + result);

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

        Path<String> sp = dag.acyclicShortestPathSequential("A", "A");

        System.out.println(sp.toString());
        System.out.println(sp.getTotalWeight());

        Path<String> newsp = dag.shortestPath("A", "I");

        System.out.println(newsp.toString());
        System.out.println(newsp.getTotalWeight());
    }

    public static class AcyclicShortestPathTask extends RecursiveTask<Integer> {
        private final int depth;

        public AcyclicShortestPathTask(int depth) {
            this.depth = depth;
        }

        @Override
        protected Integer compute() {
            if (depth <= 0) {
                return 1; // base case
            } else {
                AcyclicShortestPathTask t1 = new AcyclicShortestPathTask(depth - 1);
                AcyclicShortestPathTask t2 = new AcyclicShortestPathTask(depth - 1);
                t1.fork();
                int r2 = t2.compute();
                int r1 = t1.join();
                return r1 + r2;
            }
        }
    }
}
