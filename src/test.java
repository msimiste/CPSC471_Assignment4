/***********************************************************************
 * Author: Isai Damier
 * Title:  Bellman-Ford's Single Source Shortest Path
 * Project: geekviewpoint
 * Package: algorithm.graph
 *
 * Statement:
 *   Given a source vertex and a directed, weighted graph; find the
 *   shortest path between the source vertex and each of the other
 *   vertices. Some of the edges may have negative weights. If the graph
 *   contains any negative weight cycle, throw an exception.
 * 
 * Time-complexity: O(V*E)
 *   where V is the number of vertices and E is the number of edges.
 * 
 * The Bellman-Ford Strategy:
 * 
 *   The Bellman-Ford argument is that the longest path in any graph
 *   can have at most V-1 edges, where V is the number of vertices.
 *   Furthermore, if we perform relaxation on the set of edges once, then
 *   we will at least have determined all the one-edged shortest paths;
 *   if we traverse the set of edges twice, we will have solved at least
 *   all the two-edged shortest paths; ergo, after the V-1 iteration thru
 *   the set of edges, we will have determined all the shortest paths
 *   in the graph.
 * 
 *   But then Bellman-Ford continues: If after V-1 iterations we are still
 *   able to relax any path, then the graph has a negative edge cycle.
 * 
 *   From that argument, even before looking at any code, we can see
 *   that the time complexity is (V-1)*E +1*E = V*E. The 1*E is the
 *   cycle-detection pass.
 * 
 *   The argument also implicitly tells us that this algorithm does not
 *   mind handling negatives edges -- only negative cycles are scary.
 * 
 *   Hence, while Bellman-Ford takes longer than all versions of
 *   Dijkstra's algorithm, it has the advantage of accepting all
 *   directed graphs.
 * 
 *   I use the term relaxation a few times. Here is the explanation.
 *   The computation starts by estimating that all vertices are infinitely
 *   far away from the source vertex. Then as we compute, we relax that
 *   outrageous assumption by checking if there is actually a shorter
 *   distance. For instance, say we have already computed the shortest
 *   distance from s to v as sv and now we are seeking the solution from
 *   s to x, where x is adjacent to v. Then we know sx <= sv + vx; which
 *   simply means, either the path from s to v to x is the shortest path
 *   or there is yet a shorter path than that. If however we find that
 *   our "outrageous" estimate so far is that sx > sv+vx, then we can
 *   set sx = sv+vx as our new better estimate. That's it. That's
 *   relaxation.
 **********************************************************************/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
 
public class test{
 
  public Integer[][] singleSourceShortestPath(Integer[][] weight,
          int source) throws Exception
  {
 
    //auxiliary constants
    final int SIZE = weight.length;
    final int EVE = -1;//to indicate no predecessor
    final int INFINITY = Integer.MAX_VALUE;
 
    //declare and initialize pred to EVE and minDist to INFINITY
    Integer[] pred = new Integer[SIZE];
    Integer[] minDist = new Integer[SIZE];
    Arrays.fill(pred, EVE);
    Arrays.fill(minDist, INFINITY);
 
    //set minDist[source] = 0 because source is 0 distance from itself.
    minDist[source] = 0;
 
    //relax the edge set V-1 times to find all shortest paths
    for (int i = 1; i < minDist.length - 1; i++) {
      for (int v = 0; v < SIZE; v++) {
        for (int x : adjacency(weight, v)) {
          if (minDist[x] > minDist[v] + weight[v][x]) {
            minDist[x] = minDist[v] + weight[v][x];
            pred[x] = v;
          }
        }
      }
    }
 
    //detect cycles if any
    for (int v = 0; v < SIZE; v++) {
      for (int x : adjacency(weight, v)) {
        if (minDist[x] > minDist[v] + weight[v][x]) {
          throw new Exception("Negative cycle found");
        }
      }
    }
 
    Integer[][] result = {pred, minDist};
    return result;
  }
 
  /******************************************************************
   *  Retrieve all the neighbors of vertex v.
   *****************************************************************/
  private List<Integer> adjacency(Integer[][] G, int v) {
    List<Integer> result = new ArrayList<Integer>();
    for (int x = 0; x < G.length; x++) {
      if (G[v][x] != null) {
        result.add(x);
      }
    }
    return result;
  }
}