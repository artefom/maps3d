package Utils;

import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.Iterator;

/**
 * Created by Artem on 25.07.2016.
 */
public class LineStringInterpolatedLineIterator implements Iterator<LineSegment> {

    private LineSegment buf;
    private LineStringInterpolatedPointIterator it1;
    private LineStringInterpolatedPointIterator it2;
    private LineString ls;
    private double step;
    public LineStringInterpolatedLineIterator(LineString line, LineSegment buf, double max_length, double step) {
        this.buf = buf;
        this.ls = line;
        this.step = step;
        it1 = new LineStringInterpolatedPointIterator(line,max_length,0);
        it2 = new LineStringInterpolatedPointIterator(line,max_length,step);
    }

    @Override
    public boolean hasNext() {
        return it2.hasNext();
    }

    @Override
    public LineSegment next() {
        it2.getNextCoordinate(buf.p1);
        if (it2.hasNext()) {
            it1.getNextCoordinate(buf.p0);
        } else {
            LineSegment line;
            double back_length_buf = step;
            double pos;
            int i = -1;
            do {
                line = new LineSegment(
                        ls.getCoordinateN(ls.getNumPoints()+i),
                        ls.getCoordinateN(ls.getNumPoints()+i-1)
                );
                pos = back_length_buf/line.getLength();
                back_length_buf -= line.getLength();
                i-=1;
            } while (pos > 1);
            buf.p0 = line.pointAlong(pos);
        }
        return buf;
    }
}
