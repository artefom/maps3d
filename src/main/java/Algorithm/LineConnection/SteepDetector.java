package Algorithm.LineConnection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import java.util.Collection;

/**
 * Used to determine, whether the specific lineEnd is near steep
 */
public class SteepDetector {

    private Collection<LineString> steeps;
    private double threshold;
    private GeometryFactory gf;

    public SteepDetector(Collection<LineString> steeps, double threshold, GeometryFactory gf) {
        this.steeps = steeps;
        this.threshold = threshold;
        this.gf = gf;
    }

    public boolean isNearSteep(Point p) {
        for (LineString ls : steeps) {
            if (p.isWithinDistance(ls,threshold))
                return true;
        }
        return false;
    }

    public boolean isNearSteep(LineEnd le) {
        return isNearSteep( gf.createPoint(le.line.p1) );
    }

    public boolean isNearSteep(Connection con) {
        return isNearSteep(con.first()) || isNearSteep(con.second());
    }

}
