package Utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import java.util.LinkedList;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class Interpolator {

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

    /**
     * Interpolates line from start position to end with step size. Operates in absolute length.
     * TODO: optimize
     * @param start Start position
     * @param end End position
     * @param step Step size (real step may be a bit smaller)
     * @return
     */
    public static LinkedList<Coordinate> InterpolateAlong(LineString ls, double start, double end, double step) {
        LinkedList<Coordinate> result = new LinkedList<>();

        double length = ls.getLength();
        start = start/length;
        end = end/length;
        step = step/length;
        start = Math.min(1,Math.max(0,start));
        end = Math.min(1,Math.max(0,end));
        int iterations = (int)Math.ceil( Math.abs(end-start)/step);
        step = (end-start)/iterations;
        for (int i = 0; i <= iterations; ++i) {
            double pos = start+i*step;
            result.add(GeomUtils.pointAlong(ls,pos));
        }
        return result;
    };
}
