package Algorithm.Texture;


import Utils.CoordUtils;
import Utils.Curves.CurveString;
import Utils.GeomUtils;
import Utils.LineStringInterpolatedPointIterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleFunction;

public class NodedFunction implements DoubleFunction<Double> {


    double min_x;
    double max_x;

    // Evenly spaced y-measurements
    double[] y_cahce;
    double x_step;

    /**
     * Creates a function, from set of coordinates.
     * @param coords Coordinates, describing function.
     *               Each of following equations is always true:
     *                  this.apply(coords.get(N).x) = coords.get(N).y for any N
     *                  coords.get(N).y < coords.get(N+1).y for any N
     * @throws Exception
     */
    public NodedFunction(List<Coordinate> coords, int cacheSteps) {
        Iterator<Coordinate> it = coords.iterator();
        Coordinate c = it.next();
        if (c == null) throw new RuntimeException("Invalid coords argument");
        double prev_x = c.x;
        while (it.hasNext()) {
            c = it.next();
            if (c.x < prev_x) it.remove();
            else prev_x = c.x;
        }
        double[] xs = new double[coords.size()];
        double[] ys = new double[coords.size()];
        for (int i = 0; i != coords.size(); ++i) {
            xs[i] = coords.get(i).x;
            ys[i] = coords.get(i).y;
        }

        min_x = xs[0];
        max_x = xs[xs.length-1];
        y_cahce = new double[cacheSteps];
        for (int i = 0; i != cacheSteps; ++i) {
            double x = GeomUtils.map(i,0,cacheSteps-1,min_x,max_x);
            y_cahce[i] = getY(xs,ys,x);
        }

    }

    public static NodedFunction fromCoordinateString(String str, int cacheSteps) {
        String[] coordsinates = str.split("\\s*,\\s*");
        ArrayList<Coordinate> coords = new ArrayList<>();
        for (String coordPair : coordsinates) {
            String[] dimentions = coordPair.split("\\s+");
            double x = Double.parseDouble(dimentions[0].trim());
            double y = Double.parseDouble(dimentions[1].trim());
            coords.add(new Coordinate(x,y));
        }
        return new NodedFunction(coords,cacheSteps);
    }

    public static NodedFunction fromBezierCoordinateString(String str, int cahceSteps, double min_x, double max_x, double min_y, double max_y) {
        ArrayList<Coordinate> cs = CoordUtils.coordinatesFromString(str);
        CurveString curve = CurveString.fromCoordinates(cs);

        GeometryFactory gf = new GeometryFactory();
        LineString string = curve.interpolate(0.01,gf);
        LineStringInterpolatedPointIterator it = new LineStringInterpolatedPointIterator(string,0.01,0);
        ArrayList<Coordinate> coords = new ArrayList<>();
        while (it.hasNext()) {
            Coordinate c = it.next();
            coords.add(c);
            //System.out.print("["+c.x+", "+c.y+"], ");
        }

        CoordUtils.map(coords,min_x,max_x,min_y,max_y);

        NodedFunction nf = new NodedFunction(coords,cahceSteps);

        return nf;
    }

    private double getY(double[] xs, double[] ys, double x) {
        int start_index = xs.length-1;
        while (start_index >= 0 && xs[start_index] > x) start_index-=1;
        if (start_index < 0) return ys[0];
        if (start_index >= xs.length-1) return ys[xs.length-1];
        double d1 = x-xs[start_index];
        double d2 = xs[start_index+1]-x;
        double w1 = d2/(d1+d2);
        double w2 = d1/(d1+d2);
        return ys[start_index]*w1+ys[start_index+1]*w2;
    }

    @Override
    public Double apply(double x) {
        double index = GeomUtils.map(x,min_x,max_x,0,y_cahce.length-1);
        if (index >= y_cahce.length-1) return y_cahce[y_cahce.length-1];
        if (index <= 0) return y_cahce[0];
        double w1 = index-(int)index;
        double w2 = 1-w1;
        return y_cahce[(int)(index)]*w2+y_cahce[(int)(index)+1]*w1;
    }

}
