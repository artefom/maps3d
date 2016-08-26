package Utils.Curves;

import Deserialization.Binary.OcadVertex;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artem on 22.07.2016.
 */
public class Line extends Curve {

    Coordinate p1,p2;

    public Line() {
        p1 = new Coordinate();
        p2 = new Coordinate();
    }

    public Line(Coordinate begin, Coordinate end) {
        p1 = new Coordinate(begin);
        p2 = new Coordinate(end);
    }

    public static Line fromOcadVertices(OcadVertex begin, OcadVertex end) {
        if (begin.isBezier() || end.isBezier())
            return null;
        return new Line(begin,end);
    }

    @Override
    public void pointAlong(double pos, Coordinate buf) {

        if (pos <= 0) {
            buf.x = p1.x;
            buf.y = p1.y;
        }

        if (pos >= 1) {
            buf.x = p2.x;
            buf.y = p2.y;
        }

        buf.x = p1.x*(1-pos)+p2.x*pos;
        buf.y = p1.y*(1-pos)+p2.y*pos;
    }
}
