package Algorithm.NearbyGraph;

import Algorithm.LineConnection.Connection;
import Display.GeometryWrapper;
import Isolines.IIsoline;
import Isolines.Isoline;
import Utils.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * Finds out all nearby lines of specific line with specified precision
 */
public class NearbyEstimator {

    GeometryFactory gf;
    public NearbyEstimator(GeometryFactory gf) {
        this.gf = gf;
    }

    public void estimate(Collection<Isoline_attributed> isolines) {

        List<GeometryWrapper> gws = new ArrayList<>();

        Tracer<Isoline_attributed> tracer = new Tracer<Isoline_attributed>(isolines,(iso)->iso.getIsoline().getLineString(),gf);
        LineSegment buf = new LineSegment();

        NearbyConnectionPool p = new NearbyConnectionPool();

        //Iterate through isolines to calculate all neighbours for each isoline
        for (Isoline_attributed iso : isolines) {
            LineString ls = iso.getIsoline().getLineString();
            LineStringInterpolatedLineIterator it = new LineStringInterpolatedLineIterator(ls,
                buf, Constants.NEARBY_TRACE_STEP,Constants.NEARBY_TRACE_STEP*0.01);

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
                if (traced_positive_pair.entitiy != null && traced_positive_pair.entitiy != iso) {
                    p.add(new NearbyConnection(iso,traced_positive_pair.entitiy,1,-traced_positive_pair.side,1));
                }

                if (traced_negetive_pair.entitiy != null && traced_negetive_pair.entitiy != iso) {
                    p.add(new NearbyConnection(iso,traced_negetive_pair.entitiy,-1,-traced_negetive_pair.side,1));
                }

            }

        }

        for (NearbyConnection con : p.pool) {
            con.from.addOutcomming(con);
            con.to.addIncomming(con);
        }
    }

}
