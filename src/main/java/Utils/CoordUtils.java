package Utils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artyom.Fomenko on 18.07.2016.
 * Class for performing operations on coordinates
 */
public class CoordUtils {

    public static Coordinate sub(Coordinate c1, Coordinate c2) {
        return new Coordinate(c1.x-c2.x,c1.y-c2.y);
    }

    public static Coordinate add(Coordinate c1, Coordinate c2) {
        return new Coordinate(c1.x+c2.x,c1.y+c2.y);
    }

    public static Coordinate add(Coordinate c1, double val) { return new Coordinate(c1.x+val,c1.y+val); };
    public static Coordinate sub(Coordinate c1, double val) { return new Coordinate(c1.x-val,c1.y-val); };

    public static Coordinate mul(Coordinate c1, double val) { return new Coordinate(c1.x*val,c1.y*val); };
    public static Coordinate div(Coordinate c1, double val) { return new Coordinate(c1.x/val,c1.y/val); };

    public static Coordinate weightedAverage( Coordinate c1, Coordinate c2, double pos) {
        return add(mul(c1,(1-pos)),mul(c2,pos));
    }
}
