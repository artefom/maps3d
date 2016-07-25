package Algorithm.LineConnection;

import Utils.GeomUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
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
    private GeomUtils.Tracer<Geometry> geometryTracer;

    public Intersector(List<Geometry> primitives, GeometryFactory gf) {
        this.gf = gf;
        this.primitives = primitives;
        this.geometryTracer = new GeomUtils.Tracer<>(primitives,(p)->p,gf);
    }

    public Boolean apply(Geometry geometry) {
        for (Geometry g : primitives) {
            if (g.intersects(geometry)) return true;
        }
        return false;
    }

    public Coordinate trace(Coordinate pivot, Vector2D vec, double dist) {
        return geometryTracer.trace(pivot,vec,dist).first();
    }


}
