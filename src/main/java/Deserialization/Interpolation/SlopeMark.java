package Deserialization.Interpolation;

import Deserialization.Binary.TOcadObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;

/**
 * Created by Artem on 24.07.2016.
 */
public class SlopeMark {

    public Coordinate origin;
    public Vector2D vec;
    public double angle;

    public SlopeMark() {
        origin = null;
        vec = null;
        angle = 0;
    }

    /**
     * Initializes slope from ocad object.
     */
    public SlopeMark(TOcadObject obj ) {
        origin = obj.vertices.get(0);
        angle = Math.toRadians(obj.Ang/10);
        vec = Vector2D.create(0,1).rotate(angle);
    }

    public Coordinate pointAlong(double length) {
        return Vector2D.create(origin).add(vec.multiply(length)).toCoordinate();
    }

    public LineString asGeometry(double length, GeometryFactory gf) {
        return gf.createLineString(new Coordinate[] {origin,pointAlong(length)});
    }

}
