package ru.ogpscenter.maps3d.algorithm.NearbyGraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import ru.ogpscenter.maps3d.display.GeometryWrapper;
import ru.ogpscenter.maps3d.utils.CachedTracer;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;
import ru.ogpscenter.maps3d.utils.Constants;
import ru.ogpscenter.maps3d.utils.LineStringInterpolatedLineIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds out all nearby lines of specific line with specified precision
 */
public class NearbyEstimator {

    GeometryFactory gf;
    public NearbyEstimator(GeometryFactory gf) {
        step = Constants.NEARBY_TRACE_STEP;
        precision = Constants.NEARBY_TRACE_STEP*0.01;
        this.gf = gf;
    }

    private double step;
    private double precision;

    public static class EdgeAttributed extends DefaultWeightedEdge {

        ArrayList<Double> traces = new ArrayList<>();

        public void addTrace(double length) {
            traces.add(length);
        }

        public ArrayList<Double> getTraces() {
            return traces;
        }
    }

    public SimpleWeightedGraph<AttributedIsoline.LineSide,EdgeAttributed> getRelationGraph(NearbyContainer cont) {

        SimpleWeightedGraph<AttributedIsoline.LineSide,EdgeAttributed> ret = new SimpleWeightedGraph<>(EdgeAttributed.class);

        //Add vertecies to graph;

        for (AttributedIsoline iso : cont.getIsolines()) {
            ret.addVertex(iso.getSideNegative());
            ret.addVertex(iso.getSidePositive());
            EdgeAttributed edge = ret.addEdge(iso.getSideNegative(),iso.getSidePositive());
            ret.setEdgeWeight(edge,-100000000);
        }

        List<GeometryWrapper> gws = new ArrayList<>();

        CachedTracer<AttributedIsoline> tracer = new CachedTracer<AttributedIsoline>(cont.getIsolines(),(iso)->iso.getIsoline().getLineString(),gf);
        LineSegment buf = new LineSegment();

        int total = cont.getIsolines().size();
        int current = 0;
        //Iterate through isolines to calculate all neighbours for each isoline
        CommandLineUtils.reportProgressBegin("Tracing perpendiculars");
        for (AttributedIsoline iso : cont.getIsolines()) {
            current += 1;
            CommandLineUtils.reportProgress(current,total);

            if ( iso.getIsoline().isSteep() ) continue;
            LineString ls = iso.getIsoline().getLineString();
            LineStringInterpolatedLineIterator it = new LineStringInterpolatedLineIterator(ls,
                buf, step, precision);

            while (it.hasNext()) {
                it.next();

                // Create treace from midpoint of current lineSegment
                Coordinate trace_base = buf.midPoint();

                // Direction for detection of positive - sided slopes
                Vector2D trace_positive_vec = Vector2D.create(buf.p0,buf.p1).rotateByQuarterCircle(1).normalize();
                // Direction for detection of negative - sided slopes
                Vector2D trace_negative_vec = Vector2D.create(trace_positive_vec).negate();

                // Trace!, retrieve isolines for both sides
                CachedTracer<AttributedIsoline>.traceres traceres_positive =
                    tracer.trace(trace_base,trace_positive_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);
                CachedTracer<AttributedIsoline>.traceres  traceres_negative =
                    tracer.trace(trace_base,trace_negative_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);

                // Process  pairs, only if trace did hit something and did nont hit current isoline.

                if (traceres_positive.entitiy != null && traceres_positive.entitiy != iso && !traceres_positive.entitiy.getIsoline().isSteep() ) {
                    int from_side_index = 1;
                    int to_side_index = traceres_positive.side;
                    AttributedIsoline.LineSide from_side = iso.getSideByIndex(from_side_index);
                    AttributedIsoline.LineSide to_side = traceres_positive.entitiy.getSideByIndex(to_side_index);
                    EdgeAttributed edge = ret.getEdge(from_side,to_side);
                    if (edge == null) { // Create and initialize new edge
                        edge = ret.addEdge(from_side,to_side);
                        ret.setEdgeWeight(edge,0);
                    }
                    // Weight of connection is 1/distance, so closer isolines are more likely to be "nearby"
                    edge.addTrace(traceres_positive.distance);
                    ret.setEdgeWeight(edge,ret.getEdgeWeight(edge)-(1/(traceres_positive.distance+5)));
                }

                if (traceres_negative.entitiy != null && traceres_negative.entitiy != iso && !traceres_negative.entitiy.getIsoline().isSteep() ) {
                    int from_side_index = -1;
                    int to_side_index = traceres_negative.side;
                    AttributedIsoline.LineSide from_side = iso.getSideByIndex(from_side_index);
                    AttributedIsoline.LineSide to_side = traceres_negative.entitiy.getSideByIndex(to_side_index);
                    EdgeAttributed edge = ret.getEdge(from_side,to_side);
                    if (edge == null) { // Create and initialize new edge
                        edge = ret.addEdge(from_side,to_side);
                        ret.setEdgeWeight(edge,0);
                    }
                    // Weight of connection is 1/distance, so closer isolines are more likely to be "nearby"
                    edge.addTrace(traceres_negative.distance);
                    ret.setEdgeWeight(edge,ret.getEdgeWeight(edge)-(1/(traceres_negative.distance+5)));
                }

            }

        }
        CommandLineUtils.reportProgressEnd();

        return ret;

    }


    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        this.step = step;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }
}
