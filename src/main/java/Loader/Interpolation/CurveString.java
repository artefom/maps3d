package Loader.Interpolation;

import Loader.Binary.TDPoly;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

/**
 * Created by Artem on 22.07.2016.
 */
public class CurveString {

    ArrayList<Curve> curves;

    public CurveString() {
        curves = new ArrayList<>();
    }

    public Coordinate pointAlong(double pos) {
        int seg = (int)Math.floor(pos*curves.size());
        if (seg < 0)
            return curves.get(0).pointAlong(0);
        if (seg >= curves.size())
            return curves.get(curves.size()-1).pointAlong(1);
        return curves.get(seg).pointAlong(pos*curves.size()-seg);
    }

    public double getLength() {
        double length = 0;
        for (Curve c : curves) {
            length += c.getLength();
        }
        return length;
    }

    public CoordinateSequence getCoordinateSequence(double step, GeometryFactory gf) {
        int steps = (int)Math.ceil(getLength()/step);
        return getCoordinateSequence(steps,gf);
    }

    public CoordinateSequence getCoordinateSequence(int steps, GeometryFactory gf) {
        double step = 1.0/steps;
        Coordinate[] coords = new Coordinate[steps+1];
        for (int i = 0; i <= steps; ++i) {
            coords[i] = pointAlong(i*step);
        }
        return gf.getCoordinateSequenceFactory().create(coords);
    }

    public LineString interpolate(int steps, GeometryFactory gf) {
        double step = 1.0/steps;
        Coordinate[] coords = new Coordinate[steps+1];
        for (int i = 0; i <= steps; ++i) {
            coords[i] = pointAlong(i*step);
        }
        return gf.createLineString(coords);
    }


    public LineString interpolate(double step, GeometryFactory gf) {
        int steps = (int)Math.ceil(getLength()/step);
        if (steps < 4) steps = 4;
        return interpolate(steps,gf);
    }

    public static CurveString fromTDPoly( TDPoly[] poly ) throws Exception {
        int i = 0;
        CurveString cs = new CurveString();
        while (i < poly.length-1) {
            Curve c = null;
            if (i+3 < poly.length && (c=BezierQuadraticCurve.fromTDPoly(poly[i],poly[i+1],poly[i+2],poly[i+3]))!=null) {
                if (cs == null || cs.curves == null) {
                    cs.curves.add(c);
                }
                cs.curves.add(c);
                i += 3;
            } else if (i+2 < poly.length && (c=BezierCubicCurve.fromTDPoly(poly[i],poly[i+1],poly[i+2]))!=null) {
                cs.curves.add(c);
                i+=2;
            } else if (i+1 < poly.length && (c=Line.fromTDPoly(poly[i],poly[i+1]))!=null) {
                cs.curves.add(c);
                i+=1;
            } else {
                i += 1;
//                throw new Exception("Invalid poly array");
            }
        }
        return cs;
    }
    public static CurveString fromTDPoly(Collection<TDPoly> polyCollection) throws Exception {
        //Conver to ArrayList for random access
        return fromTDPoly(polyCollection.toArray(new TDPoly[polyCollection.size()]));
    }
}
