package Utils.GraphUtils;

import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.Test;

/**
 * Created by Artyom.Fomenko on 26.07.2016.
 */
public class GraphTest {

    @Test
    public void JGrapTTest1() {

        SimpleWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        DefaultWeightedEdge edge;

        graph.addVertex("vertex1");
        graph.addVertex("vertex2");
        graph.addVertex("vertex3");
        graph.addVertex("vertex4");
        graph.addVertex("vertex5");

        edge = graph.addEdge("vertex1", "vertex5");
        graph.setEdgeWeight(edge, -1);

        edge = graph.addEdge("vertex1", "vertex3");
        graph.setEdgeWeight(edge, -1);

        edge = graph.addEdge("vertex2", "vertex5");
        graph.setEdgeWeight(edge, -10);

        edge = graph.addEdge("vertex3", "vertex5");
        graph.setEdgeWeight(edge, -8);

        edge = graph.addEdge("vertex3", "vertex4");
        graph.setEdgeWeight(edge, -2);

        edge = graph.addEdge("vertex4", "vertex5");
        graph.setEdgeWeight(edge, -10);

        KruskalMinimumSpanningTree<String,DefaultWeightedEdge> tree = new KruskalMinimumSpanningTree<>(graph);

        for (DefaultWeightedEdge e : tree.getMinimumSpanningTreeEdgeSet()) {
            System.out.println(e);
        }

    }
}
