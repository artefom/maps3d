package Deserialization.Interpolation;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artem on 22.07.2016.
 */
public abstract class Curve {

    public Coordinate pointAlong(double pos) {
        Coordinate c = new Coordinate();
        pointAlong(pos,c);
        return c;
    }

    public abstract void pointAlong(double pos, Coordinate buf);

    public double getLength() {
        int segments = 5;
        Coordinate p1 = pointAlong(0);
        Coordinate p2 = new Coordinate();
        double length = 0;
        for (int i = 0; i <= segments; ++i) {
            double pos = (double)i/segments;
            pointAlong(pos,p2);
            length += p1.distance(p2);
            p1.setCoordinate(p2);
        }
        return length;
    }


}
