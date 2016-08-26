package Algorithm.LineConnection;

import Utils.Pair;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import java.util.Collection;

/**
 * Used to determine, whether the specific lineEnd is near steep
 *
 * (Line ends near steeps should be skipped during connection extraction algorithm to avoid corrupted connections)
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

    public double distanceToSteep( Point p ) {
        double min_dist = 100000000;
        for (LineString ls : steeps) {
            min_dist = Math.min(min_dist,p.distance(ls));
        }
        return min_dist;
    }

    public double distanceToSteep(LineEnd le) {
        return distanceToSteep( gf.createPoint(le.line.p1) );
    }

    public Pair<Double,Double> distanceToSteep(Connection con) {
        return new Pair<>( distanceToSteep(con.first()), distanceToSteep(con.second()) );
    }

}
