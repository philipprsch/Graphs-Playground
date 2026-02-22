## Graphs Playground (Java)

This repository is a java project with implementations and rigorous tests for own implementations of popular Graph algorithms, which include:

#### For Unidirected Graphs:
  - Circle Detection
  - Greedy Minimal Spanning Tree (MST), sequential
  - Boruvkas Minimal Spanning Tree (MST), parallel through streams

#### For Bipartite Graphs and Matchings
  - Validate that a graph is bipartite
  - Validate that there exists a matching in a bipartite Graph in whicha all nodes of noe of the two partitions are matched (covered)
  - Get maximal matching (between two node sets), by translating the problem to a maximal flow problem by creating an appropriate flow network

#### For Directed Graphs
  - Circle Detection with Power of Connectivity Matrix
  - Acyclic Shortest Path (min. weight), parallel through RecursiveTask and ForkJoinPool
  - Shortest Path (min. edges) with BFS
  - Acyclic Shortest Path, sequential through recursion

##### For Networks (directed)
  - Get current flow value
  - Get augmentation network
  - Calculate maximum flow (+ value)
  - Get minimal cut
  - Get minimal increasemnt required for desired flow:
    -   This algorithm, given a desired flow value, returns a Mapping of all Edges to doubles, 
        representing the incresement in capacity that is required to achive the desired maximum flow, with the lowest sum of increasements among all possible mappings.


---------------
##### Testing and Benchmarking
The repo also includes many test that I used to systematically verify the correctness of the algorithms
and benchmarking tests in which different implementations of the same algorithm and parallilization strategies are put side by side
