package ru.ogpscenter.maps3d.algorithm.NearbyGraph;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.AsUnweightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.SlopeSide;
import ru.ogpscenter.maps3d.utils.Constants;

import java.util.*;

/**
 * Created by Artyom.Fomenko on 26.07.2016.
 */
public class NearbyGraphWrapper {

    private SimpleWeightedGraph<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> graph;
    private Graph<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> graph_unweighted;
    private Set<AttributedIsoline> isolines;
    public NearbyGraphWrapper(SimpleWeightedGraph<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> graph) {
        setGraph(graph);
    }

    /**
     * Set empty closed isoline rings slope height, since almost all empty closed isoline rings are hills
     */
    public void SetHillsSlopeSides() {
        LinkedList<AttributedIsoline.LineSide> sides = new LinkedList<>();
        graph.vertexSet().forEach(sides::add);

        GeometryFactory gf = new GeometryFactory();

        for (AttributedIsoline.LineSide side : sides) {
            List<AttributedIsoline.LineSide> neighbours = Graphs.neighborListOf(graph,side);
            // If side have only one neighbour, this neighbour is other side of line. So, we found inside side of empty cirlce.
            if (neighbours.size() == 1 &&
                    side.getIsoline().getIsoline().isClosed() &&
                    side.getIsoline().getIsoline().getSlopeSide() == SlopeSide.NONE) {
                LinearRing ring = gf.createLinearRing(side.getIsoline().getIsoline().getLineString().getCoordinateSequence());
                Polygon poly = gf.createPolygon(ring);
                double area = poly.getArea();
                if (area < Constants.NEARBY_HILL_THRESHOLD_AREA) {
                    SlopeSide slopeSide = side.isPositive() ? SlopeSide.RIGHT : SlopeSide.LEFT;
                    side.getIsoline().getIsoline().setSlopeSide(slopeSide);
                }
            }
        }
    }

    public void ConvertToSpanningTree() {

        System.out.println("Creating spanning tree...");

        SimpleWeightedGraph<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> new_graph =
                new SimpleWeightedGraph<>(NearbyEstimator.EdgeAttributed.class);
        KruskalMinimumSpanningTree<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> spanningTree =
                new KruskalMinimumSpanningTree<>(graph);

        for (AttributedIsoline.LineSide side : graph.vertexSet()) {
            new_graph.addVertex(side);
        }

        for (NearbyEstimator.EdgeAttributed edge : spanningTree.getMinimumSpanningTreeEdgeSet()) {
            NearbyEstimator.EdgeAttributed new_edge = new_graph.addEdge(graph.getEdgeSource(edge),graph.getEdgeTarget(edge));
            new_graph.setEdgeWeight(new_edge, graph.getEdgeWeight(edge));
        }
        setGraph(new_graph);
    }

    private void setGraph(SimpleWeightedGraph<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> graph) {
        this.graph = graph;
        this.graph_unweighted = new AsUnweightedGraph<>(this.graph);
        isolines = new HashSet<>();
        for (AttributedIsoline.LineSide side : this.graph.vertexSet()) {
            isolines.add(side.getIsoline());
        }

    }

    public SimpleWeightedGraph<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> getGraph() {
        return graph;
    }

    private static AttributedIsoline.LineSide propagateSlope(AttributedIsoline.LineSide side1, AttributedIsoline.LineSide side2, boolean inverted) {
        IIsoline isoline1 = side1.getIsoline().getIsoline();
        IIsoline isoline2 = side2.getIsoline().getIsoline();
        if (isoline1 != isoline2) {

            if (isoline1.getSlopeSide() != SlopeSide.NONE && isoline2.getSlopeSide() == SlopeSide.NONE) {
                if (inverted) {
                    isoline2.setSlopeSide(isoline1.getSlopeSide().getOpposite());
                } else {
                    isoline2.setSlopeSide(isoline1.getSlopeSide());
                }
                return side2;
            }
            if (isoline1.getSlopeSide() == SlopeSide.NONE && isoline2.getSlopeSide() != SlopeSide.NONE) {
                if (inverted) {
                    isoline1.setSlopeSide(isoline2.getSlopeSide().getOpposite());
                } else {
                    isoline1.setSlopeSide(isoline2.getSlopeSide());
                }
                return side1;
            }

        }
        return null;
    }

    /**
     * Recover missing slope sides, this should be done before trying to recover heights
     */
    public void recoverAllSlopes() {

        System.out.println("Recovering slopes...");

        LinkedList<NearbyEstimator.EdgeAttributed> edges = new LinkedList<>();
        graph.edgeSet().forEach(edges::add);

        edges.sort( (lhs,rhs)-> Double.compare(graph.getEdgeWeight(lhs),graph.getEdgeWeight(rhs)) );
        Iterator<NearbyEstimator.EdgeAttributed> it = edges.iterator();

        while (it.hasNext()) {
            NearbyEstimator.EdgeAttributed edge = it.next();
            double current_weight = graph.getEdgeWeight(edge);
            AttributedIsoline.LineSide side1 = graph.getEdgeSource(edge);
            AttributedIsoline.LineSide side2 = graph.getEdgeTarget(edge);
            boolean inverted = side1.isPositive() == side2.isPositive();
            AttributedIsoline.LineSide propSide;
            if ( (propSide = propagateSlope(side1,side2,inverted)) != null) {
                it = edges.iterator();
                // TODO: optimize
            }
        }
        System.out.println("success.");
    }

    /**
     * Recover heights of isolines, slope side of each isoline should be known. Also, it's recommended
     * to perform a line-connection pre-processing.
     */
    public void recoverAllHeights() {

        System.out.print("Recovering heights...");
        BreadthFirstHeightRoceveryIterator it = new BreadthFirstHeightRoceveryIterator(getGraph(),isolines.iterator().next());
        while (it.hasNext()) {
            it.next();
        }
        System.out.println("success.");
    }

    public static class BreadthFirstHeightRoceveryIterator extends BreadthFirstIterator<AttributedIsoline.LineSide,NearbyEstimator.EdgeAttributed> {

        Graph<AttributedIsoline.LineSide, NearbyEstimator.EdgeAttributed> g;
        public BreadthFirstHeightRoceveryIterator(Graph<AttributedIsoline.LineSide, NearbyEstimator.EdgeAttributed> g, AttributedIsoline startLine ) {
            super(g, startLine.getSidePositive());
            this.g = g;
            super.encounterVertex(startLine.getSideNegative(),g.getEdge(startLine.getSidePositive(),startLine.getSideNegative()));
            startLine.getIsoline().setHeight(0);
            startLine.height_recovered = true;
        }

        @Override
        protected void encounterVertex(AttributedIsoline.LineSide target, NearbyEstimator.EdgeAttributed edge) {
            super.encounterVertex(target, edge);
            if (target.getIsoline().height_recovered || edge == null) return;
            AttributedIsoline.LineSide pivot = Graphs.getOppositeVertex(getGraph(),edge,target);
            if (!pivot.getIsoline().height_recovered) {
                throw new RuntimeException("Encountering vertex from vertex with unrecovered height");
            }
            double height_delta = 1;
            if (pivot.getIsoline().getIsoline().isHalf() || target.getIsoline().getIsoline().isHalf()) {
                height_delta = 0.5;
            }
            if (pivot.getIsoline().getIsoline().getSlopeSide() == SlopeSide.NONE) {
                throw new RuntimeException("Slope side undetermined!");
                //pivot.getIsoline().getIsoline().setHeight(-100000);
            }
            int mul1 = pivot.getSign() == pivot.getIsoline().getIsoline().getSlopeSide().getIntValue() ? -1 : 1;
            int mul2 = target.getSign() == target.getIsoline().getIsoline().getSlopeSide().getIntValue() ? -1 : 1;
            if (mul1 == -mul2) {
                height_delta = height_delta*mul1;
            } else {
                height_delta = 0;
            }

            target.getIsoline().getIsoline().setHeight(  pivot.getIsoline().getIsoline().getHeight()+height_delta );
            target.getIsoline().height_recovered = true;
        }
    }


}
