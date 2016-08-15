package Utils.Curves;

import Deserialization.Binary.TDPoly;
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

    public static Line fromTDPoly(TDPoly begin, TDPoly end) {
        if (begin.isBezier() || end.isBezier())
            return null;
        Coordinate p1 = begin.toCoordinate();
        Coordinate p2 = end.toCoordinate();
        return new Line(p1,p2);
    }

    @Override
    public void pointAlong(double pos, Coordinate buf) {
        buf.x = p1.x*(1-pos)+p2.x*pos;
        buf.y = p1.y*(1-pos)+p2.y*pos;
    }
}
