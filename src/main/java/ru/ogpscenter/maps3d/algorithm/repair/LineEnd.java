package ru.ogpscenter.maps3d.algorithm.repair;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import static ru.ogpscenter.maps3d.utils.CoordUtils.*;

/**
 * Represents line end. Contains end index and line segment
 *
 * Warning: public member 'other' ought to be set by AttributedIsoline, wich current {@link LineEnd} corresponds to
 */
public class LineEnd {

    public AttributedIsoline isoline;
    public int end_index;
    /**
     * {@link LineSegment} of current end. p1 - endpoint (or startpoint) of {@link AttributedIsoline}, which this {@link LineEnd} corresponds to.
     */
    public LineSegment line;

    /**
     * other end of {@link AttributedIsoline} which this {@link LineEnd} corresponds to
     */
    public LineEnd other;

    /**
     * Id of {@link MapEdge} used to calculate {@link LineEnd#isWithinEdge}
     */
    private int isWithinEdge_cached_id;

    /**
     * Value which is set by {@link ConnectionExtractor} used to cache result of determining this {@link LineEnd} begin near map edge, or pointing to it.
     */
    public boolean isWithinEdge;

    public LineEnd(AttributedIsoline isoline, LineSegment ls, int end_index) {
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

    /**
     * Get end line segment of {@link LineString}
     * @param ls {@link LineString} to extract {@link LineEnd} from
     * @param end_index index of end. 1: start, -1: end.
     * @return {@link LineSegment} whose p1 is firs coordinate of ls (when end_index = 1) or last (when end_index = -1)
     * and p0 is Vertex of ls, incident with p1.
     */
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

    /**
     * Get end line segment of {@link AttributedIsoline}
     * @param iso {@link AttributedIsoline} to extract {@link LineEnd} from
     * @param end_index index of end. 1: start, -1: end.
     * @return {@link LineSegment} whose p1 is firs coordinate of ls (when end_index = 1) or last (when end_index = -1)
     * and p0 is Vertex of iso, incident with p1.
     */
    public static LineEnd fromIsoline(AttributedIsoline iso, int end_index) {
        if (end_index == 1) {
            return iso.begin;
        }
        if (end_index == -1) {
            return iso.end;
        }
        return null;
    }

    /**
     * Reset cached value of line end being near map edge
     */
    public void isWithinEdgeCacheReset() {
        isWithinEdge_cached_id = -1;
    }

    /**
     * Determine, whether line end is near edge or points to it
     * @param edge
     * @return true if line end is too close to edge or points to it
     */
    public boolean isWithinEdge(MapEdge edge) {
        if (isWithinEdge_cached_id == edge.getID())
            return isWithinEdge;
        isWithinEdge = edge.isWithinEdge(line);
        isWithinEdge_cached_id = edge.getID();
        return isWithinEdge;
    }

    /**
     * Valid Line end satisfies following conditions:
     *
     * isoline, which current {@link LineEnd} refers to is not null
     * this.other not equals null
     * other.other equals this
     * other.isoline equals this.isoline
     *
     * @return
     */
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
