package Utils;

import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class GeomUtils {
    public static Coordinate pointAlong(LineString ls, double lineStringLengthFraction ) {
        LineSegment segment = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,segment);
        double length_buf = 0;
        double desired_length = lineStringLengthFraction*ls.getLength();
        double segment_length;
        while (it.hasNext()) {
            it.next();
            segment_length = segment.getLength();
            if (length_buf+segment_length > desired_length) {
                double local_pos = Math.max(0,Math.min(1,(desired_length-length_buf)/segment_length));
                Coordinate coord = new Coordinate();
                coord.x = segment.p0.x*(1-local_pos)+segment.p1.x*local_pos;
                coord.y = segment.p0.y*(1-local_pos)+segment.p1.y*local_pos;
                return coord;
            }
            length_buf+=segment_length;
        }
        return new Coordinate( ls.getCoordinateN(ls.getNumPoints()-1) );
    }

    public static Coordinate closestPoint(Coordinate c, LineString ls) {
        LineSegment buf = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,buf);

        Coordinate closest_point = null;
        double min_dist = Double.MAX_VALUE;

        while (it.hasNext()) {
            it.next();
            Coordinate c_cand = buf.closestPoint(c);
            double dist = c_cand.distance(c);
            if (dist < min_dist) {
                min_dist = dist;
                closest_point = c_cand;
            }
        }
        return closest_point;
    }

}
