package Utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.Iterator;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 * Iterates though LineSegments of LineString, using step
 */
public class LineStringInterpolatedIterator implements Iterator<LineSegment> {

    LineString ls;
    LineSegment buf;
    LineSegment internal_buf;
    double internal_buf_len;
    LineStringIterator iter;
    double max_length;
    int segments_num;
    double step;
    double length_buf;
    Coordinate prev_coord;
    Coordinate next_coord;
    boolean finished;

    public LineStringInterpolatedIterator(LineString ls, LineSegment buf, double max_length) {
        this.ls = ls;
        this.buf = buf;
        this.max_length = max_length;
        double len = ls.getLength();
        this.segments_num = (int)Math.ceil(len/max_length);
        this.step = len/segments_num;
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
        while (pos >= 1) {
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

    @Override
    public LineSegment next() {
        prev_coord.setCoordinate(next_coord);
        getNextCoordinate(next_coord);
        return buf;
    };
}
