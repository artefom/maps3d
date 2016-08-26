package Utils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;

import java.util.Collection;
import java.util.function.Function;

import static Utils.GeomUtils.getSide;

/**
 * Tracer traces ray from point and returns {@link traceres} containing information about entity, which this trace
 * did hit. distance to it and hitpoint.
 *
 * Entity collection is passed to tracer constructor alongside with function which extracts {@link Geometry} from generic entity
 * WARNING: Currently Entity's geometry MUST be castable to {@link LineString}
 */
public class Tracer_Legacy<T>{

    public class traceres{
        public T entitiy;
        public Coordinate point;
        //Vector2D normal;
        public int side;
        public double distance;

        @Override
        public String toString() {
            return "("+entitiy+", "+distance+", "+side+")";
        }
    }

    private Function<T,Geometry> geometryFunction;
    private Collection<T> entities;
    private GeometryFactory gf;
    public Tracer_Legacy(Collection<T> entities, Function<T,Geometry> geometryFunction, GeometryFactory gf) {
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
        double result_dist = max;
        int side;
        int prev_side;
        int result_side = 0;
        T ret = null;
        LineString boundary;
        Coordinate coord1;
        Coordinate coord2;
        CoordinateSequence ls_coords;
        for (T ent : entities) {
            LineString ls = (LineString)geometryFunction.apply(ent);

            /*TEST FOR INTERSECTION WITH BOUNDING BOX*/
            Envelope bbox = ls.getEnvelopeInternal();
            if (!((x0 >= bbox.getMinX() && x0 <= bbox.getMaxX()) &&
                    (y0 >= bbox.getMinY() && y0 <= bbox.getMaxY()))) {
                try {
                    Coordinate[] coords = ls.getEnvelope().getBoundary().getCoordinates();
                    coord1 = coords[0];
                    prev_side = getSide(vec, coord1);
                    boolean intersected = false;
                    for (int i = 1; i < coords.length; ++i) {
                        coord2 = coords[i];
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
                } catch (Exception ignored) {

                }
            }

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
                    if (t > min && t < proj_factor) {
                        result_side = side;
                        proj_factor = t;
                        ret = ent;
                        result_dist = t;
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
        result.distance = result_dist;
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
        CoordinateSequence line_coords;
        for (T ent : entities) {
            LineString ls = (LineString)geometryFunction.apply(ent);

            /*TEST FOR INTERSECTION WITH BOUNDING BOX*/
            Envelope bbox = ls.getEnvelopeInternal();
            if (!((x0 >= bbox.getMinX() && x0 <= bbox.getMaxX()) &&
                    (y0 >= bbox.getMinY() && y0 <= bbox.getMaxY()))) {

                Coordinate[] coords = ls.getEnvelope().getBoundary().getCoordinates();
                coord1 = coords[0];
                prev_side = getSide(vec, coord1);
                boolean intersected = false;
                for (int i = 1; i < coords.length; ++i) {
                    coord2 = coords[i];
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
                if (!intersected) continue;

            }

            /*TEST FOR INTERSECTION WITH LINE STRING*/

            line_coords = ls.getCoordinateSequence();
            coord1 = line_coords.getCoordinate(0);
            prev_side = getSide(vec,coord1);
            for (int i = 1; i < line_coords.size(); ++i) {
                coord2 = line_coords.getCoordinate(i);
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

    public static boolean intersects( LineString ls, LineSegment vec, double min_length_fraction, double max_length_fraction) {
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
        LineString boundary;
        Coordinate coord1;
        Coordinate coord2;
        Coordinate[] line_coords;

        /*TEST FOR INTERSECTION WITH BOUNDING BOX*/
        Envelope bbox = ls.getEnvelopeInternal();
        if (!((x0 >= bbox.getMinX() && x0 <= bbox.getMaxX()) &&
                (y0 >= bbox.getMinY() && y0 <= bbox.getMaxY()))) {

            Coordinate[] coords = ls.getEnvelope().getBoundary().getCoordinates();
            coord1 = coords[0];
            prev_side = getSide(vec, coord1);
            boolean intersected = false;
            for (int i = 1; i < coords.length; ++i) {
                coord2 = coords[i];
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
            if (intersected == false) return false;

        }

        /*TEST FOR INTERSECTION WITH LINE STRING*/

        line_coords = ls.getCoordinates();
        coord1 = line_coords[0];
        prev_side = getSide(vec,coord1);
        for (int i = 1; i < line_coords.length; ++i) {
            coord2 = line_coords[i];
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
        return false;
    }


}