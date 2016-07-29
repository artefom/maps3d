package Algorithm.LineConnection;

import Utils.CoordUtils;
import Utils.GeomUtils;
import Utils.Pair;
import Utils.Tracer;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;

import java.util.List;
import java.util.function.Function;

/**
 * Created by Artem on 20.07.2016.
 * Test geometry for intersection with any of it's content
 */
public class Intersector {

    private List<Geometry> primitives;
    private GeometryFactory gf;
    private Tracer<Geometry> geometryTracer;

    public Intersector(List<Geometry> primitives, GeometryFactory gf) {
        this.gf = gf;
        this.primitives = primitives;
        this.geometryTracer = new Tracer<>(primitives,(p)->p,gf);
    }

    public boolean intersects(LineSegment seg) {
        return geometryTracer.intersects(seg,0.01,0.99);
    };

    public Coordinate trace(Coordinate pivot, Vector2D vec,double min_dist, double max_dist) {
        Tracer<Geometry>.traceres ret_pair = geometryTracer.trace(pivot, vec,min_dist,max_dist);
        if (ret_pair.entitiy == null) return null;
        return ret_pair.point;
    }

}
