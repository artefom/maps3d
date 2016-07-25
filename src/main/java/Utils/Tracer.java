package Utils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;

import java.util.Collection;
import java.util.function.Function;

import static Utils.GeomUtils.getSide;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class Tracer<T>{

    public class traceres{
        public T entitiy;
        public Coordinate point;
        //Vector2D normal;
        public int side;
    }

    private Function<T,Geometry> geometryFunction;
    private Collection<T> entities;
    private GeometryFactory gf;
    public Tracer(Collection<T> entities, Function<T,Geometry> geometryFunction, GeometryFactory gf) {
        this.entities = entities;
        this.geometryFunction = geometryFunction;
        this.gf = gf;
    }

    public traceres trace(Coordinate pivot, Vector2D vec, double min, double max) {
        return fastTrace(new LineSegment(pivot,vec.add(Vector2D.create(pivot)).toCoordinate()),min,max);
    }

    public traceres fastTrace(LineSegment vec, double min, double max) {
        double x0 = vec.p0.x;
        double y0 = vec.p0.y;
        double vx = vec.p1.x-x0;
        double vy = vec.p1.y-y0;
        double proj_factor = max;
        double a;
        double b;
        double c;
        double t;
        int side;
        int prev_side;
        int result_side = 0;
        T ret = null;
        LineString boundary;
        Coordinate coord1;
        Coordinate coord2;
        for (T ent : entities) {
            LineString ls = (LineString)geometryFunction.apply(ent);

            /*TEST FOR INTERSECTION WITH BOUNDING BOX*/
            Envelope bbox = ls.getEnvelopeInternal();
            if (!((x0 > bbox.getMinX() && x0 < bbox.getMaxX()) &&
                    (y0 > bbox.getMinY() && y0 < bbox.getMaxY()))) {
                boundary = (LineString) ls.getEnvelope().getBoundary();
                coord1 = boundary.getCoordinateN(0);
                prev_side = getSide(vec, coord1);
                boolean intersected = false;
                for (int i = 1; i < boundary.getNumPoints(); ++i) {
                    coord2 = boundary.getCoordinateN(i);
                    side = getSide(vec, coord2);
                    if (prev_side != side) {
                        a = coord1.y - coord2.y;
                        b = coord2.x - coord1.x;
                        c = coord1.x * coord2.y - coord2.x * coord1.y;
                        t = -(c + a * x0 + b * y0) / (a * vx + b * vy);
                        if (t > 0 && t < proj_factor) {
                            intersected = true;
                            break;
                        }
                    }
                    prev_side = side;
                    coord1 = coord2;
                }
                if (intersected == false) continue;
            }

            /*TEST FOR INTERSECTION WITH LINE STRING*/

            coord1 = ls.getCoordinateN(0);
            prev_side = getSide(vec,coord1);
            for (int i = 1; i < ls.getNumPoints(); ++i) {
                coord2 = ls.getCoordinateN(i);
                side = getSide(vec,coord2);
                if (prev_side != side) {
                    result_side = prev_side;
                    a = coord1.y-coord2.y;
                    b = coord2.x-coord1.x;
                    c = coord1.x*coord2.y-coord2.x*coord1.y;
                    t = -(c+a*x0+b*y0)/(a*vx+b*vy);
                    if (t > min && t < proj_factor) {
                        proj_factor = t;
                        ret = ent;
                    }
                }
                prev_side = side;
                coord1 = coord2;
            }
        }
        traceres result = new traceres();
        result.entitiy = ret;
        result.point = vec.pointAlong(proj_factor);
        result.side = result_side;
        return result;
    }

    public boolean intersects( LineSegment vec, double min_length_fraction, double max_length_fraction) {
        double x0 = vec.p0.x;
        double y0 = vec.p0.y;
        double vx = vec.p1.x-x0;
        double vy = vec.p1.y-y0;
        double proj_factor = max_length_fraction;
        double a;
        double b;
        double c;
        double t;
        double prev_side;
        T ret = null;
        LineString boundary;
        Coordinate coord1;
        Coordinate coord2;
        for (T ent : entities) {
            LineString ls = (LineString)geometryFunction.apply(ent);

            /*TEST FOR INTERSECTION WITH BOUNDING BOX*/
            Envelope bbox = ls.getEnvelopeInternal();
            if (!((x0 > bbox.getMinX() && x0 < bbox.getMaxX()) &&
                    (y0 > bbox.getMinY() && y0 < bbox.getMaxY()))) {
                boundary = (LineString) ls.getEnvelope().getBoundary();
                coord1 = boundary.getCoordinateN(0);
                prev_side = getSide(vec, coord1);
                boolean intersected = false;
                for (int i = 1; i < boundary.getNumPoints(); ++i) {
                    coord2 = boundary.getCoordinateN(i);
                    int side = getSide(vec, coord2);
                    if (prev_side != side) {
                        a = coord1.y - coord2.y;
                        b = coord2.x - coord1.x;
                        c = coord1.x * coord2.y - coord2.x * coord1.y;
                        t = -(c + a * x0 + b * y0) / (a * vx + b * vy);
                        if (t > 0 && t < proj_factor) {
                            intersected = true;
                            break;
                        }
                    }
                    prev_side = side;
                    coord1 = coord2;
                }
                if (intersected == false) continue;
            }

            /*TEST FOR INTERSECTION WITH LINE STRING*/

            coord1 = ls.getCoordinateN(0);
            prev_side = getSide(vec,coord1);
            for (int i = 1; i < ls.getNumPoints(); ++i) {
                coord2 = ls.getCoordinateN(i);
                int side = getSide(vec,coord2);
                if (prev_side != side) {
                    a = coord1.y-coord2.y;
                    b = coord2.x-coord1.x;
                    c = coord1.x*coord2.y-coord2.x*coord1.y;
                    t = -(c+a*x0+b*y0)/(a*vx+b*vy);
                    if (t > min_length_fraction && t < proj_factor) {
                        return true;
                    }
                }
                prev_side = side;
                coord1 = coord2;
            }
        }
        return false;
    }

    public static boolean intersects(LineSegment s1, LineSegment s2, double min_percent_length, double max_percent_length) {
        double x0 = s1.p0.x;
        double y0 = s1.p0.y;
        double vx = s1.p1.x-x0;
        double vy = s1.p1.y-y0;
        double a;
        double b;
        double c;
        double t;
        double prev_side = getSide(s1,s2.p0);
        double side = getSide(s1,s2.p1);
        if (side == prev_side) return false;
        a = s2.p0.y-s2.p1.y;
        b = s2.p1.x-s2.p0.x;
        c = s2.p0.x*s2.p1.y-s2.p1.x*s2.p0.y;
        t = -(c+a*x0+b*y0)/(a*vx+b*vy);
        return t > min_percent_length && t < max_percent_length;
    }

}