package Algorithm.LineConnection;

import Isolines.IsolineContainer;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Evaluates score of connection.
 * Connections with higher score are more likely to be connected
 */
public class ConnectionEvaluator implements Function<Connection, Double> {

    double max_angle;
    double min_angle;
    double max_length;
    double weld_dist;

    public ConnectionEvaluator(double min_angle, double max_angle, double max_length, double weld_dist) {
        this.min_angle = min_angle;
        this.max_angle = max_angle;
        this.max_length = max_length;
        this.weld_dist = weld_dist;
    }


    /**
     * Score of two lines being parallel 0 - perpendicular 1 - parallel
     * @return
     */
    public static double parallelScore(Connection con) {
        if (con.first().isoline == con.second().isoline)
            return 0;

        Isoline_attributed i1 = con.first().isoline;
        Isoline_attributed i2 = con.second().isoline;

        List<Coordinate> ep1 = IsolineContainer.getEndPointArray(i1.getLineString(),con.first().end_index,5,10);
        List<Coordinate> ep2 = IsolineContainer.getEndPointArray(i2.getLineString(),con.second().end_index,5,10);
        Iterator<Coordinate> it1 = ep1.iterator();
        Iterator<Coordinate> it2 = ep2.iterator();

        int count = 0;
        double squares_accum = 0;
        double accum = 0;

        while (it1.hasNext() && it2.hasNext()) {
            double d = it1.next().distance(it2.next());
            count += 1;
            accum += d;
            squares_accum += d*d;
        }

        double std = Math.sqrt( squares_accum/count - (accum/count)*(accum/count) );
        return std;
    }

    private double evaluateLineToPoint(Coordinate l1, Coordinate l2, Coordinate c) {
        double ang = Math.PI- Angle.angleBetween(l1,l2,c);
        ang = ((max_angle*0.5)-ang)/(max_angle*0.5);
        double dist = l2.distance(c);
        if (dist < weld_dist)
            return 10;
        if (dist > max_length)
            return -10;
        dist = (max_length-dist)/max_length;
        return (dist+ang)*0.5;
        //return -1;
    }

    @Override
    public Double apply(Connection connection) {
//        if (connection.first().line == connection.second().line) {
//        }
//        LineSegment line1 = connection.first().line;
//        LineSegment line2 = connection.second().line;
//        if (line1 == null || line2 == null) return -1.0;
//        return (max_length-connection.first().line.p1.distance(connection.second().line.p1))/max_length;
        LineSegment line1 = connection.first().line;
        LineSegment line2 = connection.second().line;
        if (line1 == null || line2 == null) return -1.0;
        return (evaluateLineToPoint(line1.p0,line1.p1,line2.p1)+
                evaluateLineToPoint(line2.p0,line2.p1,line1.p1))*0.5;
    }

    public void evaluate(Collection<Connection> cons) {
        for (Connection con: cons) {
            if (con != null) {
                if (con.isValid()) {
                    con.score = apply(con);
                    //con.score = -1;
                } else {
                    con.score = -1;
                }
            }
        }
    }
}