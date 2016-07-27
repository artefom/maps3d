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
        for (Isoline_attributed iso : cont.getIsolines()) {
            ret.addVertex(iso.getSideNegative());
            ret.addVertex(iso.getSidePositive());
            DefaultWeightedEdge edge = ret.addEdge(iso.getSideNegative(),iso.getSidePositive());
            ret.setEdgeWeight(edge,-100000000);
        }

        List<GeometryWrapper> gws = new ArrayList<>();

        Tracer<Isoline_attributed> tracer = new Tracer<Isoline_attributed>(cont.getIsolines(),(iso)->iso.getIsoline().getLineString(),gf);
        LineSegment buf = new LineSegment();

        //Iterate through isolines to calculate all neighbours for each isoline
        for (Isoline_attributed iso : cont.getIsolines()) {
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
                Tracer<Isoline_attributed>.traceres traced_positive_pair =
                    tracer.trace(trace_base,trace_positive_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);
                Tracer<Isoline_attributed>.traceres  traced_negetive_pair =
                    tracer.trace(trace_base,trace_negative_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);

                // Process  pairs, only if trace did hit something and did nont hit current isoline.

                if (traced_positive_pair.entitiy != null && traced_positive_pair.entitiy != iso && !traced_positive_pair.entitiy.getIsoline().isSteep() ) {
                    int from_side_index = 1;
                    int to_side_index = traced_positive_pair.side;
                    Isoline_attributed.LineSide from_side = iso.getSideByIndex(from_side_index);
                    Isoline_attributed.LineSide to_side = traced_positive_pair.entitiy.getSideByIndex(to_side_index);
                    DefaultWeightedEdge edge = ret.getEdge(from_side,to_side);
                    if (edge == null) {
                        edge = ret.addEdge(from_side,to_side);
                        ret.setEdgeWeight(edge,0);
                    }
                    ret.setEdgeWeight(edge,ret.getEdgeWeight(edge)-1);
                }

                if (traced_negetive_pair.entitiy != null && traced_negetive_pair.entitiy != iso && !traced_negetive_pair.entitiy.getIsoline().isSteep() ) {
                    int from_side_index = -1;
                    int to_side_index = traced_negetive_pair.side;
                    Isoline_attributed.LineSide from_side = iso.getSideByIndex(from_side_index);
                    Isoline_attributed.LineSide to_side = traced_negetive_pair.entitiy.getSideByIndex(to_side_index);
                    DefaultWeightedEdge edge = ret.getEdge(from_side,to_side);
                    if (edge == null) {
                        edge = ret.addEdge(from_side,to_side);
                        ret.setEdgeWeight(edge,0);
                    }
                    ret.setEdgeWeight(edge,ret.getEdgeWeight(edge)-1);
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
