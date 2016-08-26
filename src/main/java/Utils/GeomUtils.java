package Utils;

import com.vividsolutions.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

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
     * Get side of (x3 y3) relative to Line segment( x0 y0, x0+v0x y0+v0y)
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x3
     * @param y3
     * @return
     */
    public static int getSide( double x0, double y0, double v0x, double v0y, double x3, double y3) {
        double v2x = x3 - x0;
        double v2y = y3 - y0;
        return ( (v0x*v2y - v0y*v2x) > 0) ? 1 : -1;
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

    public static Coordinate getInteriorPoint(Polygon p) {
        Envelope e = p.getEnvelopeInternal();
        LineSegment bisector = new LineSegment(new Coordinate(e.getMinX(),(e.getMinY()+e.getMaxY())*0.5), new Coordinate(e.getMaxX(),(e.getMinY()+e.getMaxY())*0.5));

        List<Double> intersectionPoints = getAllIntersectionProjectionFactors(bisector,p.getExteriorRing(),0,1);
        for (int i = 0; i != p.getNumInteriorRing(); ++i) {
            intersectionPoints.addAll( getAllIntersectionProjectionFactors(bisector,p.getInteriorRingN(i),0,1) );
        }
        intersectionPoints.sort((lhs,rhs)->Double.compare(lhs,rhs));
        if (intersectionPoints.size()%2 != 0) throw new RuntimeException("Invalid polygon");
        double maxDist = 0;
        double pfactor1 = 0;
        double pfactor2 = 0;
        for (int i = 0; i < intersectionPoints.size(); i+=2) {
            double projFactor1 = intersectionPoints.get(i);
            double projFactor2 = intersectionPoints.get(i+1);
            double dist = projFactor2-projFactor1;
            if (dist > maxDist) {
                maxDist = dist;
                pfactor1 = projFactor1;
                pfactor2 = projFactor2;
            }
        }
        return bisector.pointAlong((pfactor1+pfactor2)*0.5);
    }

    public static List<Double> getAllIntersectionProjectionFactors(LineSegment vec, LineString ls, double min, double max) {
        double x0 = vec.p0.x;
        double y0 = vec.p0.y;
        double vx = vec.p1.x-x0;
        double vy = vec.p1.y-y0;
        double a;
        double b;
        double c;
        double t;
        int side;
        int prev_side;
        Coordinate coord1;
        Coordinate coord2;
        CoordinateSequence ls_coords;
        List<Double> result = new ArrayList<>();

        /*TEST FOR INTERSECTION WITH LINE STRING*/

        ls_coords = ls.getCoordinateSequence();
        coord1 = ls_coords.getCoordinate(0);
        prev_side = getSide(vec,coord1);
        for (int i = 1; i < ls_coords.size(); ++i) {
            coord2 = ls_coords.getCoordinate(i);
            side = getSide(vec,coord2);
            if (prev_side != side) {
                a = coord1.y-coord2.y;
                b = coord2.x-coord1.x;
                c = coord1.x*coord2.y-coord2.x*coord1.y;
                t = -(c+a*x0+b*y0)/(a*vx+b*vy);
                result.add(t);
            }
            prev_side = side;
            coord1 = coord2;
        }
        return result;
    }

    public static double clamp(double val, double from, double to) {
        return Math.max(from, Math.min(to,val));
    }

    public static int clamp(int val, int from, int to) {
        return Math.max(from, Math.min(to,val));
    }

    public static short clamp(short val, short from, short to) {
        return (short)Math.max(from, Math.min(to,val));
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
