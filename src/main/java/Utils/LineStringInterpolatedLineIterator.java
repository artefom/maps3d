package Utils;

import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.Iterator;

/**
 * Iterates through LineString with LineSegments of length 'segmentLength' and step 'max_step_length'
 *
 * segmentLength is supposed to be very small (like 0.0001, when total line length is 20-30), so
 * this iterator produces tiny line segments that can be used to determine line direction in line segment's midpoint
 *
 * this iterator was needed, because it's not possible to recover line direction in each point, using {@link LineStringInterpolatedPointIterator}
 * It is also too inaccurate when using {@link LineStringInterpolatedIterator}
 */
public class LineStringInterpolatedLineIterator implements Iterator<LineSegment> {

    private LineSegment buf;
    private LineStringInterpolatedPointIterator it1;
    private LineStringInterpolatedPointIterator it2;
    private LineString ls;
    private double segmentLength;
    public LineStringInterpolatedLineIterator(LineString line, LineSegment buf, double max_step_length, double segmentLength) {
        this.buf = buf;
        this.ls = line;
        this.segmentLength = segmentLength;
        it1 = new LineStringInterpolatedPointIterator(line,max_step_length,0);
        it2 = new LineStringInterpolatedPointIterator(line,max_step_length,segmentLength);
    }

    @Override
    public boolean hasNext() {
        return it2.hasNext();
    }

    /**
     * Returns reference to internal buffer, whose coordinates are being updated
     *
     * DOES NOT CREATE NEW LINESEGMENT ON EACH STEP! USED FOR PERFORMANCE OPTIMIZATION
     * @return
     */
    @Override
    public LineSegment next() {
        it2.getNextCoordinate(buf.p1);
        if (it2.hasNext()) {
            it1.getNextCoordinate(buf.p0);
        } else {
            LineSegment line;
            double back_length_buf = segmentLength;
            double pos = 0;
            int i = -1;
            do {
                line = new LineSegment(
                        ls.getCoordinateN(ls.getNumPoints()+i),
                        ls.getCoordinateN(ls.getNumPoints()+i-1)
                );
                pos = back_length_buf/line.getLength();
                back_length_buf -= line.getLength();
                i-=1;
            } while (pos > 1 && ls.getNumPoints()+i-1 >= 0);
            buf.p0 = line.pointAlong(pos);
        }
        return buf;
    }
}
