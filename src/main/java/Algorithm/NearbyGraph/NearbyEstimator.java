package Algorithm.NearbyGraph;

import Algorithm.LineConnection.Connection;
import Display.GeometryWrapper;
import Isolines.IIsoline;
import Isolines.Isoline;
import Utils.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import javafx.scene.paint.Color;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

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

    public SimpleWeightedGraph<Isoline_attributed.LineSide,DefaultWeightedEdge> getRelationGraph(NearbyContainer cont) {

        SimpleWeightedGraph<Isoline_attributed.LineSide,DefaultWeightedEdge> ret = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        //Add vertecies to graph;
        System.out.println("Recovering edges...");
        for (Isoline_attributed iso : cont.getIsolines()) {
            ret.addVertex(iso.getSideNegative());
            ret.addVertex(iso.getSidePositive());
            DefaultWeightedEdge edge = ret.addEdge(iso.getSideNegative(),iso.getSidePositive());
            ret.setEdgeWeight(edge,-100000000);
        }

        List<GeometryWrapper> gws = new ArrayList<>();

        Tracer<Isoline_attributed> tracer = new Tracer<Isoline_attributed>(cont.getIsolines(),(iso)->iso.getIsoline().getLineString(),gf);
        LineSegment buf = new LineSegment();

        System.out.println("Tracing perpendiculars");
        int total = cont.getIsolines().size();
        int current = 0;
        int percent_prev = -1;
        //Iterate through isolines to calculate all neighbours for each isoline
        for (Isoline_attributed iso : cont.getIsolines()) {
            current += 1;
            int percent = (int)Math.round((double)current/total*100);
            if (percent_prev != percent) {
                System.out.println(percent+"%, "+current+" of "+total);
                percent_prev = percent;
            }

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
                Tracer<Isoline_attributed>.traceres traceres_positive =
                    tracer.trace(trace_base,trace_positive_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);
                Tracer<Isoline_attributed>.traceres  traceres_negative =
                    tracer.trace(trace_base,trace_negative_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);

                // Process  pairs, only if trace did hit something and did nont hit current isoline.

                if (traceres_positive.entitiy != null && traceres_positive.entitiy != iso && !traceres_positive.entitiy.getIsoline().isSteep() ) {
                    int from_side_index = 1;
                    int to_side_index = traceres_positive.side;
                    Isoline_attributed.LineSide from_side = iso.getSideByIndex(from_side_index);
                    Isoline_attributed.LineSide to_side = traceres_positive.entitiy.getSideByIndex(to_side_index);
                    DefaultWeightedEdge edge = ret.getEdge(from_side,to_side);
                    if (edge == null) {
                        edge = ret.addEdge(from_side,to_side);
                        ret.setEdgeWeight(edge,0);
                    }
                    // Weight of connection is 1/distance, so closer isolines are more likely to be "nearby"
                    ret.setEdgeWeight(edge,ret.getEdgeWeight(edge)-(1/(traceres_positive.distance+5)));
                }

                if (traceres_negative.entitiy != null && traceres_negative.entitiy != iso && !traceres_negative.entitiy.getIsoline().isSteep() ) {
                    int from_side_index = -1;
                    int to_side_index = traceres_negative.side;
                    Isoline_attributed.LineSide from_side = iso.getSideByIndex(from_side_index);
                    Isoline_attributed.LineSide to_side = traceres_negative.entitiy.getSideByIndex(to_side_index);
                    DefaultWeightedEdge edge = ret.getEdge(from_side,to_side);
                    if (edge == null) {
                        edge = ret.addEdge(from_side,to_side);
                        ret.setEdgeWeight(edge,0);
                    }
                    // Weight of connection is 1/distance, so closer isolines are more likely to be "nearby"
                    ret.setEdgeWeight(edge,ret.getEdgeWeight(edge)-(1/(traceres_negative.distance+5)));
                }

            }

        }

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
