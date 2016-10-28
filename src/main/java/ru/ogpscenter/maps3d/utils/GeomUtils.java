package ru.ogpscenter.maps3d.utils;

import com.vividsolutions.jts.geom.*;
import ru.ogpscenter.maps3d.isolines.SlopeSide;

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
     * @param seg segment
     * @param point point
     * @return 1 if point is to the LEFT from seg, -1 if point is to the RIGHT from seg (looking from seg.p0 to seg.p1)
     */
    public static SlopeSide getSide(LineSegment seg, Coordinate point) {
        // segment vector
        double v1x = seg.p1.x - seg.p0.x;
        double v1y = seg.p1.y - seg.p0.y;
        // vector to point from segment origin
        double v2x = point.x - seg.p0.x;
        double v2y = point.y - seg.p0.y;
        return ((v1x * v2y - v1y * v2x) > 0) ? SlopeSide.LEFT : SlopeSide.RIGHT;
    }

    /**
     * Get side of (x3 y3) relative to Line segment( x0 y0, x0+v0x y0+v0y)
     * @return
     */
    public static SlopeSide getSide( double x0, double y0, double v0x, double v0y, double x3, double y3) {
        double v2x = x3 - x0;
        double v2y = y3 - y0;
        return ( (v0x*v2y - v0y*v2x) > 0) ? SlopeSide.LEFT : SlopeSide.RIGHT;
    }

    public static double crossProduct(double v0x, double v0y, double v1x, double v1y) {
        return (v0x*v1y - v0y*v1x);
    }

    /**
     * unsigned area of triangle
     * @param c0
     * @param c1
     * @param c2
     * @return
     */
    public static double area(Coordinate c0, Coordinate c1, Coordinate c2) {
        double v0x = c2.x-c0.x;
        double v0y = c2.y-c0.y;
        double v1x = c1.x-c0.x;
        double v1y = c1.y-c0.y;
        return Math.abs(crossProduct(v0x,v0y,v1x,v1y));
    }


    /**
     * Returns length fraction of closest point along {@link LineString} lineString to {@link Coordinate} c
     * @param c
     * @param lineString
     * @return length fraction
     */
    public static double projectionFactor( Coordinate c, LineString lineString ) {
        LineSegment lineSegment = new LineSegment();
        double minDist = c.distance(lineString.getCoordinateN(0));
        double minLength = 0;
        double totalLength = 0;
        for (int i = 1; i < lineString.getNumPoints(); ++i) {
            lineSegment.p0 = lineString.getCoordinateN(i-1);
            lineSegment.p1 = lineString.getCoordinateN(i);
            double segmentLength = lineSegment.getLength();
            double distanceFromSegment = lineSegment.distance(c);
            if (distanceFromSegment < minDist) {
                minDist = distanceFromSegment;
                minLength = totalLength + segmentLength * lineSegment.projectionFactor(c);
            }
            totalLength += segmentLength;
        }
        return minLength / totalLength;
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
        SlopeSide side;
        SlopeSide prevSide;
        Coordinate coord1;
        Coordinate coord2;
        CoordinateSequence ls_coords;
        List<Double> result = new ArrayList<>();

        /*TEST FOR INTERSECTION WITH LINE STRING*/

        ls_coords = ls.getCoordinateSequence();
        coord1 = ls_coords.getCoordinate(0);
        prevSide = getSide(vec,coord1);
        for (int i = 1; i < ls_coords.size(); ++i) {
            coord2 = ls_coords.getCoordinate(i);
            side = getSide(vec,coord2);
            if (prevSide != side) {
                a = coord1.y-coord2.y;
                b = coord2.x-coord1.x;
                c = coord1.x*coord2.y-coord2.x*coord1.y;
                t = -(c+a*x0+b*y0)/(a*vx+b*vy);
                result.add(t);
            }
            prevSide = side;
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
