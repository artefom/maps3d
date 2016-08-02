package Deserialization.Interpolation;

import Deserialization.Binary.TDPoly;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artem on 21.07.2016.
 */
public class BezierQuadraticCurve extends Curve {

    Coordinate p1,b1,b2,p2;

    public BezierQuadraticCurve() {
        p1 = new Coordinate();
        p2 = new Coordinate();
        b1 = new Coordinate();
    }

    public BezierQuadraticCurve(Coordinate begin, Coordinate bezier1, Coordinate bezier2, Coordinate end) {
        p1 = begin;
        b1 = bezier1;
        b2 = bezier2;
        p2 = end;
    }

    public static BezierQuadraticCurve fromTDPoly(TDPoly begin, TDPoly bezier1, TDPoly bezier2, TDPoly end) {
        if (begin.isBezier() || !bezier1.isBezier() || !bezier2.isBezier() || end.isBezier())
            return null;
        Coordinate p1 = begin.toCoordinate();
        Coordinate b1 = bezier1.toCoordinate();
        Coordinate b2 = bezier2.toCoordinate();
        Coordinate p2 = end.toCoordinate();
        return new BezierQuadraticCurve(p1,b1,b2,p2);
    }

    @Override
    public void pointAlong(double pos, Coordinate buf) {
        double pos1 = 1-pos;
        buf.x = ((p1.x*pos1+b1.x*pos)*pos1+(b1.x*pos1+b2.x*pos)*pos)*pos1+((b1.x*pos1+b2.x*pos)*pos1+(b2.x*pos1+p2.x*pos)*pos)*pos;
        buf.y = ((p1.y*pos1+b1.y*pos)*pos1+(b1.y*pos1+b2.y*pos)*pos)*pos1+((b1.y*pos1+b2.y*pos)*pos1+(b2.y*pos1+p2.y*pos)*pos)*pos;
    }
}
