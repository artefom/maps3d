package Algorithm.EdgeDetection;

import Algorithm.LineConnection.Connection;
import Algorithm.LineConnection.Intersector;
import Algorithm.LineConnection.LineEnd;
import Isolines.IsolineContainer;
import Utils.Constants;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import org.opensphere.geometry.algorithm.*;
import org.opensphere.geometry.triangulation.*;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Artem on 21.07.2016.
 */
public class Edge {

    public LinearRing outerBound;
    public GeometryFactory gf;
    public Intersector intersector;
    public double max_dist;
    private int id;

    private static int edge_id = 0;

    private Edge(GeometryCollection gc,double threshold, GeometryFactory gf, Intersector intersector) {
        this.gf = gf;
        this.intersector = intersector;
        Polygon chull = (Polygon)(new ConcaveHull(gc,threshold).getConcaveHull());
        outerBound = (LinearRing) chull.getExteriorRing();
        double height = chull.getEnvelopeInternal().getHeight();
        double width = chull.getEnvelopeInternal().getWidth();
        max_dist = Math.sqrt(height*height+width*width);
        id = edge_id++;
    }

    public static Edge fromGeometryCollection( Collection<Geometry> gc, GeometryFactory gf, Intersector intersector, double threshold) {
        GeometryCollection gc2 = new GeometryCollection(gc.toArray(new Geometry[gc.size()]),gf);
        return new Edge(gc2,threshold, gf, intersector);
    }

    public static Edge fromIsolines(IsolineContainer isos, double threshold) {
        Intersector intersector = new Intersector( isos.getIsolinesAsGeometry(), isos.getFactory() );
        return fromGeometryCollection(isos.stream().map((x)->x.getGeometry()).collect(Collectors.toList()),
                isos.getFactory(), intersector, threshold);
    }

    public boolean isWithinEdge(Coordinate c) {
        return gf.createPoint(c).isWithinDistance(outerBound, Constants.EDGE_WITHIN_THRESHOLD);
    }

    public boolean isWithinEdge(LineSegment ls) {
        if (isWithinEdge(ls.p1)) return true;
        Coordinate traced = intersector.trace(
                Vector2D.create(ls.p1).add(Vector2D.create(ls.p0,ls.p1).multiply(0.01)).toCoordinate(),
                Vector2D.create(ls.p0,ls.p1),max_dist);
        if (traced == null) return true;
        if (gf.createLineString(new Coordinate[]{ls.p1, traced}).intersects(outerBound)) {
            return true;
        }
        return  false;
    }

    public boolean isWithinEdge(Connection con) {
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

//    public boolean isEdgeToEdge(LineString ls) {
//        LineSegment beg = new LineSegment(ls.getCoordinateN(1),ls.getCoordinateN(0));
//        LineSegment end = new LineSegment(ls.getCoordinateN(ls.getNumPoints()-2),ls.getCoordinateN(ls.getNumPoints()-1));
//        if (isWithinEdge(beg) && isWithinEdge(end)) return true;
//        return false;
//    }
}
