package Algorithm.EdgeDetection;

import Algorithm.LineConnection.Connection;
import Algorithm.LineConnection.LineEnd;
import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.*;
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
    private Edge(GeometryCollection gc,double threshold, GeometryFactory gf) {
        Polygon chull = (Polygon)(new ConcaveHull(gc,threshold).getConcaveHull());
        outerBound = (LinearRing) chull.getExteriorRing();
    }

    public static Edge fromGeometryCollection( Collection<Geometry> gc, GeometryFactory gf, double threshold) {
        GeometryCollection gc2 = new GeometryCollection(gc.toArray(new Geometry[gc.size()]),gf);
        return new Edge(gc2,threshold, gf);
    }

    public static Edge fromIsolines(IsolineContainer isos, double threshold) {
        return fromGeometryCollection(isos.stream().map((x)->x.getGeometry()).collect(Collectors.toList()),
                isos.getFactory(),threshold);
    }

    public boolean isWithinEdge(Coordinate c) {
        return false;
    }

    public boolean isWithinEdge(LineEnd le) {
        return false;
    }

    public boolean isWithinEdge(Connection con) {
        return false;
    }

    public boolean isEdgeToEdge(LineString ls) {
        return false;
    }
}
