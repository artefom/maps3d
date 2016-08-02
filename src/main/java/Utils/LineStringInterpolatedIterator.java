package Utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.Iterator;

/**
 * Iterates through lineString with LineSegment of fixed length.
 *
 * example:
 * iterating through line(0,0 - 0,10) with max_length = 1 (see {@link LineStringInterpolatedIterator#LineStringInterpolatedIterator(LineString, LineSegment, double)}
 * will give following line segments:
 * 0,0 - 0,1
 * 0,1 - 0,2
 * 0,2 - 0,3
 * ...
 * 0,9 - 0,10
 *
 * WARNING: segment's length may not equal max_length, but guaranteed to be less
 * segment length is calculated as follows: ls.getLength() / ( (int)Math.ceil(ls.getLength() / max_length )
 */
public class LineStringInterpolatedIterator implements Iterator<LineSegment> {

    private LineSegment buf;
    private LineSegment internal_buf;
    private double internal_buf_len;
    private LineStringIterator iter;
    private double step;
    private double length_buf;
    private Coordinate prev_coord;
    private Coordinate next_coord;
    private boolean finished;

    public LineStringInterpolatedIterator(LineString ls, LineSegment buf, double max_length) {
        this.buf = buf;
        double len = ls.getLength();
        int segments_num = (int) Math.ceil(len / max_length);
        this.step = len/ segments_num;
        this.internal_buf = new LineSegment();
        this.iter = new LineStringIterator(ls,internal_buf);
        iter.next();
        this.internal_buf_len = internal_buf.getLength();
        prev_coord = new Coordinate( 0,0 );
        next_coord = new Coordinate( internal_buf.p0 );
        finished = false;
        buf.p0 = prev_coord;
        buf.p1 = next_coord;
    }

    private Coordinate getNextCoordinate(Coordinate buf) {
        length_buf+=step;
        double pos = length_buf/internal_buf_len;
        while (pos >= 0.99999999) {
            length_buf-=internal_buf_len;
            if (iter.hasNext()) {
                iter.next();
            } else {
                pos = 1;
                finished = true;
                break;
            }
            internal_buf_len = internal_buf.getLength();
            pos = length_buf/internal_buf_len;
        };
        buf.x = internal_buf.p0.x*(1-pos)+internal_buf.p1.x*pos;
        buf.y = internal_buf.p0.y*(1-pos)+internal_buf.p1.y*pos;
        return buf;
    }

    @Override
    public boolean hasNext() {
        return !finished;
    }

    /**
     * Returns reference to internal buffer, whose coordinates are being updated
     *
     * DOES NOT CREATE NEW LINESEGMENT ON EACH STEP! USED FOR PERFORMANCE OPTIMIZATION
     * @return
     */
    @Override
    public LineSegment next() {
        prev_coord.setCoordinate(next_coord);
        getNextCoordinate(next_coord);
        return buf;
    };
}
