package Utils.Area;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.ArrayList;

/**
 * Created by Artyom.Fomenko on 22.08.2016.
 */
public class LineSegmentWrapper {

    CoordinateSequence cs;
    int segment_id;

    public LineSegmentWrapper(CoordinateSequence cs, int sement_id) {
        this.cs = cs;
        this.segment_id = sement_id;
    }

    public LineSegment getSegment() {
        return new LineSegment(cs.getCoordinate(segment_id),cs.getCoordinate(segment_id+1));
    }

    public void getSegment(LineSegment buf) {
        buf.p0 = cs.getCoordinate(segment_id);
        buf.p1 = cs.getCoordinate(segment_id+1);
    }

    public static ArrayList<LineSegmentWrapper> fromLineString(LineString ls) {
        ArrayList<LineSegmentWrapper> ret = new ArrayList<>();
        CoordinateSequence cs = ls.getCoordinateSequence();
        int size = cs.size()-1;
        for (int i = 0; i != size; ++i) {
            ret.add(new LineSegmentWrapper(cs,i));
        }
        return ret;
    }

    public double getBeginX() {
        return cs.getX(segment_id);
    }

    public double getBeginY() {
        return cs.getY(segment_id);
    }

    public double getEndX() {
        return cs.getX(segment_id+1);
    }

    public double getEndY() {
        return cs.getY(segment_id+1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LineSegmentWrapper that = (LineSegmentWrapper) o;

        if (segment_id != that.segment_id) return false;
        return cs != null ? cs.equals(that.cs) : that.cs == null;

    }

    @Override
    public int hashCode() {
        int result = cs != null ? cs.hashCode() : 0;
        result = 31 * result + segment_id;
        return result;
    }
}
