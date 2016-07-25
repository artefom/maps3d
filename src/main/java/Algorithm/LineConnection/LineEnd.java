package Algorithm.LineConnection;

import Algorithm.EdgeDetection.Edge;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import static Utils.CoordUtils.add;
import static Utils.CoordUtils.div;
import static Utils.CoordUtils.sub;

/**
 * Represents line end. Contains end index and line segment
 */

public class LineEnd {

    public Isoline_attributed isoline;
    public int end_index;
    public LineSegment line;
    public LineEnd other;

    private int isWithinEdge_cached_id;
    public boolean isWithinEdge;

    public LineEnd(Isoline_attributed isoline, LineSegment ls, int end_index) {
        this.isoline = isoline;
        this.end_index = end_index;
        this.line = ls;
        isWithinEdge = false;
        isWithinEdge_cached_id = -1;
    }

    public LineEnd() {
        isoline = null;
        end_index = 0;
        line = null;
        isWithinEdge = false;
        isWithinEdge_cached_id = -1;
    }

    public static LineSegment getEnd(LineString ls, int end_index) {
        if (ls.isClosed()) return null;
        Coordinate begin;
        Coordinate end;
        if (end_index == -1) {
            begin = ls.getCoordinateN(ls.getNumPoints()-2);
            end = ls.getCoordinateN(ls.getNumPoints()-1);
        } else {
            begin = ls.getCoordinateN(1);
            end = ls.getCoordinateN(0);
        }
        begin = sub(begin,end);
        begin = div(begin, Math.sqrt(begin.x*begin.x+begin.y*begin.y) );
        begin = add(begin,end);
        return new LineSegment(begin,end);
    }

    public static LineEnd fromIsoline(Isoline_attributed iso, int end_index) {
        if (end_index == 1) {
            return iso.begin;
        }
        if (end_index == -1) {
            return iso.end;
        }
        return null;
    }

    public void isWithinEdgeCacheReset() {
        isWithinEdge_cached_id = -1;
    }
    public boolean isWithinEdge(Edge edge) {
        if (isWithinEdge_cached_id == edge.getID())
            return isWithinEdge;
        isWithinEdge = edge.isWithinEdge(line);
        isWithinEdge_cached_id = edge.getID();
        return isWithinEdge;
    }

    public boolean isValid() {
        if (this.isoline != null && this.other != null && other.isoline == this.isoline && other.other == this)
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return isoline.hashCode()+end_index;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Connection)) return false;
        LineEnd other = (LineEnd)obj;
        return this.isoline == other.isoline && this.end_index == other.end_index && this.other == other.other;
    }

    @Override
    public String toString() {
        return "LE( "+end_index+" "+isoline+")";
    }

}
