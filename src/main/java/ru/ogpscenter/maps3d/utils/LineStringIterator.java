package ru.ogpscenter.maps3d.utils;

import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.Iterator;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 * Iterates through LineSegments of LineString
 */
public class LineStringIterator implements Iterator<LineSegment> {

    private LineSegment buffer;
    private LineString ls;
    private int current_pos;

    public LineStringIterator(LineString ls, LineSegment buffer) {
        this.buffer = buffer;
        this.ls = ls;
        this.current_pos = 0;
    }

    @Override
    public boolean hasNext() {
        return current_pos < ls.getNumPoints()-1;
    }

    @Override
    public LineSegment next() {
        buffer.p0 = ls.getCoordinateN(current_pos);
        current_pos += 1;
        buffer.p1 = ls.getCoordinateN(current_pos);
        return buffer;
    }

}
