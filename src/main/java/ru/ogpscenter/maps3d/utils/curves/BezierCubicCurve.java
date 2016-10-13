package ru.ogpscenter.maps3d.utils.curves;

import Deserialization.Binary.OcadVertex;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artem on 22.07.2016.
 */
public class BezierCubicCurve extends Curve {

    Coordinate p1,b1,p2;

    public BezierCubicCurve() {
        p1 = new Coordinate();
        p2 = new Coordinate();
        b1 = new Coordinate();
    }

    public BezierCubicCurve(Coordinate begin, Coordinate bezier, Coordinate end) {
        p1 = new Coordinate(begin);
        b1 = new Coordinate(bezier);
        p2 = new Coordinate(end);
    }

    public static BezierCubicCurve fromOcadVertices(OcadVertex begin, OcadVertex bezier, OcadVertex end) {
        if (begin.isBezier() || !bezier.isBezier() || end.isBezier())
            return null;
        return new BezierCubicCurve(begin,bezier,end);
    }

    @Override
    public void pointAlong(double pos, Coordinate buf) {
        double pos1 = 1-pos;
        buf.x = (p1.x*pos1+b1.x*pos)*pos1+(b1.x*pos1+p2.x*pos)*pos;
        buf.y = (p1.y*pos1+b1.y*pos)*pos1+(b1.y*pos1+p2.y*pos)*pos;
    }
}
