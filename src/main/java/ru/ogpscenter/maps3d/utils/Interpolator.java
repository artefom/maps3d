package ru.ogpscenter.maps3d.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import java.util.LinkedList;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class Interpolator {

    /**
     * Get sub-string of {@link LineString} starting from length-fraction 'start' and ending at length-fraction 'end' with
     * step 'step'. Resulting sub-string with have (int)Math.ceil( Math.abs(end-start)/step) points.
     * @param ls
     * @param start
     * @param end
     * @param step
     * @return
     */
    public static LinkedList<Coordinate> InterpolateAlongLocal(LineString ls, double start, double end, double step ) {
        start = Math.min(1,Math.max(0,start));
        end = Math.min(1,Math.max(0,end));
        LinkedList<Coordinate> result = new LinkedList<>();
        int iterations = (int)Math.ceil( Math.abs(end-start)/step);
        step = (end-start)/iterations;
        for (int i = 0; i <= iterations; ++i) {
            double pos = start+i*step;
            result.add(GeomUtils.pointAlong(ls,pos));
        }
        return result;
    };

}
