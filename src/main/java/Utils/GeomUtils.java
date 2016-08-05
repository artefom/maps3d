package Utils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

/**
 * Class, containing static 2D geometry functions
 */
public class GeomUtils {

    /**
     * Point along line string
     * @param ls
     * @param lineStringLengthFraction fraction of length
     * @return
     */
    public static Coordinate pointAlong(LineString ls, double lineStringLengthFraction ) {
        LineSegment segment = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,segment);
        double length_buf = 0;
        double desired_length = lineStringLengthFraction*ls.getLength();
        double segment_length;
        while (it.hasNext()) {
            it.next();
            segment_length = segment.getLength();
            if (length_buf+segment_length > desired_length) {
                double local_pos = Math.max(0,Math.min(1,(desired_length-length_buf)/segment_length));
                Coordinate coord = new Coordinate();
                coord.x = segment.p0.x*(1-local_pos)+segment.p1.x*local_pos;
                coord.y = segment.p0.y*(1-local_pos)+segment.p1.y*local_pos;
                return coord;
            }
            length_buf+=segment_length;
        }
        return new Coordinate( ls.getCoordinateN(ls.getNumPoints()-1) );
    }

    /**
     * @param seg
     * @param c
     * @return 1 if c is to the LEFT from seg, -1 if c is to the RIGHT from seg (looking from seg.p0 to seg.p1)
     */
    public static int getSide( LineSegment seg, Coordinate c ) {
        double v1x = seg.p1.x-seg.p0.x;
        double v1y = seg.p1.y-seg.p0.y;
        double v2x = c.x - seg.p0.x;
        double v2y = c.y - seg.p0.y;
        return ( (v1x*v2y - v1y*v2x) > 0) ? 1 : -1;
    }


    /**
     * returns length fraction of closest point along {@link LineString} ls to {@link Coordinate} c
     * @param c
     * @param ls
     * @return length fraction
     */
    public static double projectionFactor( Coordinate c, LineString ls ) {
        LineSegment seg = new LineSegment();
        double min_dist = c.distance(ls.getCoordinateN(0));
        double min_length = 0;
        double length_accum = 0;
        for (int i = 1; i < ls.getNumPoints(); ++i) {
            seg.p0 = ls.getCoordinateN(i-1);
            seg.p1 = ls.getCoordinateN(i);
            double len = seg.getLength();
            double d = seg.distance(c);
            if (d < min_dist) {
                min_dist = d;
                min_length = length_accum + len*seg.projectionFactor(c);
            }
            length_accum += len;
        }
        return min_length/length_accum;
    }

    /**
     * Returns closest point on {@link LineString} ls to {@link Coordinate} c
     * @param c
     * @param ls
     * @return Point, lying on ls
     */
    public static Coordinate closestPoint(Coordinate c, LineString ls) {
        LineSegment buf = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,buf);

        Coordinate closest_point = null;
        double min_dist = Double.MAX_VALUE;

        while (it.hasNext()) {
            it.next();
            Coordinate c_cand = buf.closestPoint(c);
            double dist = c_cand.distance(c);
            if (dist < min_dist) {
                min_dist = dist;
                closest_point = c_cand;
            }
        }
        return closest_point;
    }

    public static double clamp(double val, double from, double to) {
        return Math.max(from, Math.min(to,val));
    }

    public static int clamp(int val, int from, int to) {
        return Math.max(from, Math.min(to,val));
    }

    /**
     * map value from range (inMin-inMax) to range (outMin-outMax)
     * @param value value to be mapped
     * @param inMin first interval minimum value
     * @param inMax first interval maximum value
     * @param outMin second interval minimum value
     * @param outMax second interval maximum value
     * @return outMin + (value - inMin)*(outMax - outMin)/(inMax - inMin)
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (value - inMin)*(outMax - outMin)/(inMax - inMin);
    }
}
