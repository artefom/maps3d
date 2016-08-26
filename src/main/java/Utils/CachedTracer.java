package Utils;

import Utils.Area.GeometryAreaBuffer;
import Utils.Area.LSWAttributed;
import Utils.Area.LineSegmentWrapper;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;

import java.util.*;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import static Utils.GeomUtils.getSide;

/**
 * Tracer traces ray from point and returns {@link traceres} containing information about entity, which this trace
 * did hit. distance to it and hitpoint.
 *
 * Entity collection is passed to tracer constructor alongside with function which extracts {@link Geometry} from generic entity
 * WARNING: Currently Entity's geometry MUST be castable to {@link LineString}
 */
public class CachedTracer<T>{

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
    public GeometryAreaBuffer buffer;
    private Collection<T> entities;
    private GeometryFactory gf;
    private ArrayList<LineSegmentWrapper> wrappers;

//    private void addEntity(Collection<LineSegmentWrapper> col, T entity) {
//        Geometry g_col = geometryFunction.apply(entity);
//
//        LSWAttributed.fromGeometry(entity,g_col).forEach(col::add);
//
//    }

    public CachedTracer(Collection<T> entities, Function<T,Geometry> geometryFunction, GeometryFactory gf) {
        this.entities = entities;
        this.geometryFunction = geometryFunction;
        this.gf = gf;
        this.buffer = new GeometryAreaBuffer();

        wrappers = new ArrayList<>();

        for (T e : entities) {
            Geometry g_col = geometryFunction.apply(e);
            LSWAttributed.fromGeometry(e,g_col).forEach(wrappers::add);
        }

        buffer.setEnvelope(wrappers,500,500);
        buffer.addAll(wrappers);
    }

    public traceres trace(Coordinate pivot, Vector2D vec, double min, double max) {
        return fastTrace(new LineSegment(pivot,vec.add(Vector2D.create(pivot)).toCoordinate()),min,max);
    }

    public traceres fastTrace(LineSegment vec, double min, double max) {
        double x0 = vec.p0.x;
        double y0 = vec.p0.y;
        double vx = vec.p1.x-x0;
        double vy = vec.p1.y-y0;

        return fastTrace(x0,y0,vx,vy,min,max);
    }

    Set<LineSegmentWrapper> intersection_candidates_buf = Collections.newSetFromMap(new IdentityHashMap<LineSegmentWrapper,Boolean>());

    public traceres fastTrace(double x0,double y0,double vx,double vy, double min, double max) {
        double a;
        double b;
        double c;
        double t;
        int side;
        int prev_side;

        traceres res = new traceres();

        res.distance = max;
        res.entitiy = null;
        res.point = null;
        res.side = 0;

        intersection_candidates_buf.clear();
        buffer.findPossiblyIntersecting(x0+vx*min,y0+vy*min,x0+vx*max,y0+vy*max,intersection_candidates_buf);

        //ArrayList<LineSegmentWrapper> intersection_candidates = wrappers;

        //LineSegment ls = new LineSegment( new Coordinate(x0,y0), new Coordinate(x0+vx,y0+vy));

        for (LineSegmentWrapper lsw_raw : intersection_candidates_buf) {
            LSWAttributed<T> lsw = (LSWAttributed<T>)lsw_raw;

            prev_side = getSide(x0,y0,vx,vy,lsw.getBeginX(),lsw.getBeginY());
            side = getSide(x0,y0,vx,vy,lsw.getEndX(),lsw.getEndY());

            if (prev_side != side) {

                a = lsw.getBeginY()-lsw.getEndY();
                b = lsw.getEndX()-lsw.getBeginX();
                c = lsw.getBeginX()*lsw.getEndY()-lsw.getEndX()*lsw.getBeginY();
                t = -(c+a*x0+b*y0)/(a*vx+b*vy);

                if (t >= min && t <= res.distance) {
                    res.distance = t;
                    res.entitiy = lsw.entity;
                    res.side = side;
                }
            }
        }

        res.point = new Coordinate(x0+vx*res.distance,y0+vy*res.distance);

        return res;
    }

    public void getAllIntersections(Polygon p, ArrayList< Pair<LSWAttributed<T>,Double> > ret ) {
        LineString exterior_ring = p.getExteriorRing();
        getAllIntersections(exterior_ring,ret);

        for (int i = 0; i != p.getNumInteriorRing(); ++i) {
            getAllIntersections(p.getInteriorRingN(i),ret);
        }
    }

    public void getAllIntersections(LineString ls, ArrayList< Pair<LSWAttributed<T>,Double> > ret) {

        LineSegment buf = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,buf);

        while (it.hasNext()){
            it.next();
            getAllIntersections(buf,0,1,ret);
        }
    }

    public void getAllIntersections(LineSegment ls, double min, double max, ArrayList< Pair<LSWAttributed<T>,Double> > ret) {
        double x0 = ls.p0.x;
        double y0 = ls.p0.y;
        double vx = ls.p1.x-x0;
        double vy = ls.p1.y-y0;
        getAllIntersections(x0,y0,vx,vy,min,max,ret);
    }

    public void getAllIntersections(double x0,double y0,double vx,double vy, double min, double max, ArrayList< Pair<LSWAttributed<T>,Double> > ret) {
        double a;
        double b;
        double c;
        double t;
        int side;
        int prev_side;

        intersection_candidates_buf.clear();
        buffer.findPossiblyIntersecting(x0+vx*min,y0+vy*min,x0+vx*max,y0+vy*max,intersection_candidates_buf);


        for (LineSegmentWrapper lsw_raw : intersection_candidates_buf) {
            LSWAttributed<T> lsw = (LSWAttributed<T>)lsw_raw;

            prev_side = getSide(x0,y0,vx,vy,lsw.getBeginX(),lsw.getBeginY());
            side = getSide(x0,y0,vx,vy,lsw.getEndX(),lsw.getEndY());

            if (prev_side != side) {

                a = lsw.getBeginY()-lsw.getEndY();
                b = lsw.getEndX()-lsw.getBeginX();
                c = lsw.getBeginX()*lsw.getEndY()-lsw.getEndX()*lsw.getBeginY();
                t = -(c+a*x0+b*y0)/(a*vx+b*vy);

                if (t >= min && t <= max) {
                    ret.add(new Pair<>(lsw,t));
                }
            }
        }
    }

    public boolean intersects( LineSegment vec, double min_length_fraction, double max_length_fraction) {
        double x0 = vec.p0.x;
        double y0 = vec.p0.y;
        double vx = vec.p1.x-x0;
        double vy = vec.p1.y-y0;
        return intersects(x0,y0,vx,vy,min_length_fraction,max_length_fraction);
    }

    public boolean intersects( double x0,double y0,double vx,double vy, double min, double max ) {

        double a;
        double b;
        double c;
        double t;
        int side;
        int prev_side;

        intersection_candidates_buf.clear();
        buffer.findPossiblyIntersecting(x0+vx*min,y0+vy*min,x0+vx*max,y0+vy*max,intersection_candidates_buf);

        for (LineSegmentWrapper lsw_raw : intersection_candidates_buf) {
            LSWAttributed<T> lsw = (LSWAttributed<T>)lsw_raw;

            prev_side = getSide(x0,y0,vx,vy,lsw.getBeginX(),lsw.getBeginY());
            side = getSide(x0,y0,vx,vy,lsw.getEndX(),lsw.getEndY());

            if (prev_side != side) {

                a = lsw.getBeginY()-lsw.getEndY();
                b = lsw.getEndX()-lsw.getBeginX();
                c = lsw.getBeginX()*lsw.getEndY()-lsw.getEndX()*lsw.getBeginY();
                t = -(c+a*x0+b*y0)/(a*vx+b*vy);

                if (t >= min && t <= max) {
                    return true;
                }
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
//
//    public static double intersectionProjFactor(LineSegment s1, LineSegment s2) {
//        double x0 = s1.p0.x;
//        double y0 = s1.p0.y;
//        double vx = s1.p1.x-x0;
//        double vy = s1.p1.y-y0;
//        double a = s2.p0.y-s2.p1.y;
//        double b = s2.p1.x-s2.p0.x;
//        double c = s2.p0.x*s2.p1.y-s2.p1.x*s2.p0.y;
//        return -(c+a*x0+b*y0)/(a*vx+b*vy);
//    }

    public static boolean intersects( LineString ls, LineSegment vec, double min_length_fraction, double max_length_fraction) {
        double x0 = vec.p0.x;
        double y0 = vec.p0.y;
        double vx = vec.p1.x-x0;
        double vy = vec.p1.y-y0;
        //double proj_factor = max_length_fraction;
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
                    if (t > 0 && t < max_length_fraction) {
                        intersected = true;
                        break;
                    }
                }
                prev_side = side;
                coord1 = coord2;
            }
            if (!intersected) return false;

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
                if (t > min_length_fraction && t < max_length_fraction) {
                    return true;
                }
            }
            prev_side = side;
            coord1 = coord2;
        }
        return false;
    }

}