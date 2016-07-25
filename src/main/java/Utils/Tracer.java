package Utils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;

import java.util.Collection;
import java.util.function.Function;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class Tracer<T>{

    private Function<T,Geometry> geometryFunction;
    private Collection<T> entities;
    private GeometryFactory gf;
    public Tracer(Collection<T> entities, Function<T,Geometry> geometryFunction, GeometryFactory gf) {
        this.entities = entities;
        this.geometryFunction = geometryFunction;
        this.gf = gf;
    }

    public Pair<Coordinate,T> trace(Coordinate pivot, Vector2D vec, double dist) {
        return trace(pivot,vec,0,dist,(x)->true);
    }

    public Pair<Coordinate,T> trace(Coordinate pivot, Vector2D vec, double offset, double dist, Function<T,Boolean> mask) {
        Coordinate c = null;
        T ret = null;
        vec = Vector2D.create(vec).normalize();
        Coordinate real_pivot = Vector2D.create(pivot).add(vec.multiply(offset)).toCoordinate();
        LineString line = gf.createLineString( new Coordinate[] {
                real_pivot, vec.multiply(dist).add(Vector2D.create(pivot)).toCoordinate()});
        for (T t : entities) {
            Geometry g = geometryFunction.apply(t);
            if (g.intersects( line )) {
                if (mask.apply(t)) {
                    Geometry intersecion_points = line.intersection(g);
                    for (int i = 0; i != intersecion_points.getNumGeometries(); ++i) {
                        LineString new_line = gf.createLineString(new Coordinate[]{real_pivot, c});
                        if (new_line.getLength() < line.getLength()) {
                            Point p = (Point) intersecion_points.getGeometryN(i);
                            ret = t;
                            c = p.getCoordinate();
                            line = new_line;
                        }
                    }
                }
            }

        }
        return new Pair<>(c, ret);
    }
    public Pair<Coordinate,T> trace(Coordinate pivot, Vector2D vec, double dist, Function<T,Boolean> mask) {
        return trace(pivot,vec, 0, dist, mask);
    }
}