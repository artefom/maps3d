package Algorithm.LineConnection;

import Isolines.Isoline;
import Isolines.IsolineContainer;
import Utils.CachedTracer;
import Utils.Constants;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import org.opensphere.geometry.algorithm.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Wrapper around Concave hull algorithm to calculate concave hull of isoline collection.
 */
public class MapEdge {

    public LinearRing outerBound;
    public LinearRing outerRectangle;
    public GeometryFactory gf;
    public CachedTracer<Geometry> intersector;
    public double max_dist;
    private int id;

    private static int edge_id = 0;

    private MapEdge(GeometryCollection gc, double threshold, GeometryFactory gf, CachedTracer<Geometry> intersector) {
        this.gf = gf;
        this.intersector = intersector;
        Polygon concaveHull = (Polygon)(new ConcaveHull(gc,threshold).getConcaveHull());
        outerBound = (LinearRing) concaveHull.getExteriorRing();
        outerRectangle = (LinearRing) ((Polygon) concaveHull.getEnvelope()).getExteriorRing();
        double height = concaveHull.getEnvelopeInternal().getHeight();
        double width = concaveHull.getEnvelopeInternal().getWidth();
        max_dist = Math.sqrt(height*height+width*width);
        id = edge_id++;
    }

    public static Polygon getConvexHull(IsolineContainer ic) {
        ArrayList<Geometry> gc = ic.getIsolinesAsGeometry();
        GeometryCollection gc2 = new GeometryCollection(gc.toArray(new Geometry[gc.size()]),ic.getFactory());
        ConvexHull chull = new ConvexHull(gc2);
        return (Polygon)chull.getConvexHull();
    }

    public static Polygon getConcaveHull(IsolineContainer ic) {
        ArrayList<Geometry> gc = ic.getIsolinesAsGeometry();
        GeometryCollection gc2 = new GeometryCollection(gc.toArray(new Geometry[gc.size()]),ic.getFactory());
        return (Polygon)(new ConcaveHull(gc2,Constants.EDGE_CONCAVE_THRESHOLD).getConcaveHull());
    }
    public static Polygon getConcaveHull(Geometry g, double threshold) {
        return (Polygon)(new ConcaveHull(g,threshold).getConcaveHull());
    }

    public static MapEdge fromGeometryCollection(Collection<Geometry> gc, GeometryFactory gf, CachedTracer<Geometry> intersector, double threshold) {
        GeometryCollection gc2 = new GeometryCollection(gc.toArray(new Geometry[gc.size()]),gf);
        return new MapEdge(gc2,threshold, gf, intersector);
    }

    public static MapEdge fromIsolines(IsolineContainer isos, double threshold) {
        CachedTracer<Geometry> intersector = new CachedTracer<>( isos.getIsolinesAsGeometry(), (x)->x, isos.getFactory());
        return fromGeometryCollection(isos.stream().map((x)->x.getGeometry()).collect(Collectors.toList()),
                isos.getFactory(), intersector, threshold);
    }


    private boolean isWithinEdge(Coordinate c) {
        return gf.createPoint(c).isWithinDistance(outerBound, Constants.EDGE_WITHIN_THRESHOLD);
    }

    /**
     * Determine, whether coordinate is too close to edge
     * Used and cached by {@link LineEnd#isWithinEdge}.
     * @param ls
     * @return
     */
    boolean isWithinEdge(LineSegment ls) {
        if (isWithinEdge(ls.p1)) return true;
        Coordinate begin = ls.p1;
        Vector2D vec = Vector2D.create(ls.p0,ls.p1).normalize();
        CachedTracer.traceres traced = intersector.trace(begin,vec,0.01,max_dist);
        if (traced == null) return true;
        if (gf.createLineString(new Coordinate[]{ls.p1, traced.point}).intersects(outerBound)) {
            return true;
        }
        return  false;
    }

    private boolean isWithinEdge(Connection con) {
        if (con.first().isWithinEdge(this) || con.second().isWithinEdge(this)) return true;
        return false;
    }


    /**
     * When caching lines realationship to the end, edge id will be used to determine
     * if calculating relationship with same edge as before.
     */
    public int getID() {
        return id;
    }

}
