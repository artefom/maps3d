package ru.ogpscenter.maps3d.utils.curves;


import Deserialization.Binary.OcadVertex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;
import ru.ogpscenter.maps3d.utils.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by Artem on 22.07.2016.
 */
public class CurveString {

    ArrayList<Curve> curves;

    public CurveString() {
        curves = new ArrayList<>();
    }

    public Coordinate pointAlong(double pos) {
        if (pos <= 0) {
            return curves.get(0).pointAlong(0);
        }
        if (pos >= 1) {
            return curves.get(curves.size()-1).pointAlong(1);
        }
        int seg = (int)Math.floor(pos*curves.size());
        if (seg < 0)
            return curves.get(0).pointAlong(0);
        if (seg >= curves.size())
            return curves.get(curves.size()-1).pointAlong(1);
        return curves.get(seg).pointAlong(pos*curves.size()-seg);
    }

    public Pair<Curve,Double> localPointAlong(double pos){

        if (pos <= 0) {
            return new Pair<>(curves.get(0),0.0);
        }
        if (pos >= 1) {
            return new Pair<>(curves.get(curves.size()-1),1.0);
        }
        int seg = (int)Math.floor(pos*curves.size());
        if (seg < 0)
            return new Pair<>(curves.get(0),0.0);
        if (seg >= curves.size())
            return new Pair<>(curves.get(curves.size()-1),1.0);

        return new Pair<>(curves.get(seg),pos*curves.size()-seg);
    }

    public double getLength() {
        double length = 0;
        for (Curve c : curves) {
            length += c.getLength();
        }
        return length;
    }

    private CoordinateSequence getCoordinateSequence(double step, GeometryFactory gf) {
        int steps = (int)Math.ceil(getLength()/step);
        return getCoordinateSequence(steps,gf);
    }

    private CoordinateSequence getCoordinateSequence(int steps, GeometryFactory gf) {
        if (steps < 4) steps = 4; // Handle rings.
        double step = 1.0/steps;
        Coordinate[] coords = new Coordinate[steps+1];

        // Manually set start and endpoint, because due to precision loss linarRing's end and begin can deviate after interpolation
        coords[0] = pointAlong(0);
        for (int i = 1; i != steps; ++i) {
            coords[i] = pointAlong(i*step);
        }
        coords[steps] = pointAlong(1);
        return gf.getCoordinateSequenceFactory().create(coords);
    }


//    private LineString interpolate(int steps, GeometryFactory gf) {
//
////        int pre_steps = steps*10;
////        double pre_step = 1.0/pre_steps;
////        Coordinate[] coords = new Coordinate[pre_steps+1];
////        for (int i = 0; i <= pre_steps; ++i) {
////            coords[i] = pointAlong(i*pre_step);
////        }
////        LineString pre_ls = gf.createLineString(coords);
////        //return pre_ls;
////        ArrayList<Coordinate> resultCoords = new ArrayList<>();
////        LineStringInterpolatedPointIterator it = new LineStringInterpolatedPointIterator(pre_ls,pre_ls.getLength()/steps,0);
////        while (it.hasNext()) {
////            Coordinate next = it.next();
////            resultCoords.add(next);
////        }
////        return gf.createLineString(resultCoords.toArray(new Coordinate[resultCoords.size()]));
//    }


    public void interpolate(Collection<Coordinate> result_coordinates, double crease_angle) {

        int pre_steps = 10;
        double pre_step = 1.0/pre_steps;

        ArrayList<Coordinate> coords = new ArrayList<>();
        for (Curve c : curves) {
            coords.add(c.pointAlong(0));

            if (c instanceof Line)
                continue;
            for (int i = 1; i < pre_steps; ++i) {
                coords.add(c.pointAlong(i*pre_step));
            }
        }
        coords.add(curves.get(curves.size()-1).pointAlong(1));

        Coordinate[] fit_coordinates = coords.toArray(new Coordinate[coords.size()]);
        Vector2D prev_vector = Vector2D.create(fit_coordinates[0],fit_coordinates[1]);
        Coordinate prev_point = fit_coordinates[0];
        result_coordinates.add(prev_point);
        int prev_i = 0;

        double angle_accum =  0;
        for (int i = 1; i < fit_coordinates.length; ++i) {
            Coordinate cur_point = fit_coordinates[i];
            Vector2D cur_vector = Vector2D.create(fit_coordinates[i-1],cur_point);


            angle_accum += prev_vector.angleTo(cur_vector);
            if (Math.abs( angle_accum ) > crease_angle) {
                angle_accum = 0;
                Coordinate p1 = prev_point;
                Coordinate p2 = cur_point;

                Coordinate max_p3 = null;
                double max_angle = 0;
                for (int j = prev_i; j < i; ++j) {

                    Coordinate p3 = fit_coordinates[j];

                    Vector2D vec1 = Vector2D.create(p1,p3);
                    Vector2D vec2 = Vector2D.create(p3,p2);
                    double angle = Math.abs(vec1.angleTo(vec2));
                    if (angle > max_angle) {
                        angle = max_angle;
                        max_p3 = p3;
                    }

                }
                if (max_p3 != null) {
                    p2 = max_p3;
                }
                result_coordinates.add(p2);
                prev_point = cur_point;
                prev_i = i;
            }
            prev_vector = cur_vector;
        }

        result_coordinates.add(fit_coordinates[fit_coordinates.length-1]);
    }

    public LineString interpolate(GeometryFactory gf) {

        ArrayList<Coordinate> interpolate_coords = new ArrayList<>();
        interpolate(interpolate_coords,0.25);

        return gf.createLineString( interpolate_coords.toArray(new Coordinate[interpolate_coords.size()]) );
    }

    public static CurveString fromCoordinates(Coordinate[] coordinates) {
        CurveString cs = new CurveString();
        if (coordinates.length == 2) {
            cs.curves.add(new Line(coordinates[0],coordinates[1]));
            return cs;
        }
        if (coordinates.length == 3) {
            cs.curves.add(new BezierCubicCurve(coordinates[0],coordinates[1],coordinates[2]));
            return cs;
        }
        if ( coordinates.length % 3 != 1) return null;
        for (int i = 0; i < (coordinates.length-1); i += 3) {
            cs.curves.add(new BezierQuadraticCurve(
                    coordinates[i],
                    coordinates[i+1],
                    coordinates[i+2],
                    coordinates[i+3]));
        }
        return cs;
    }

    public static CurveString fromCoordinatesLinear(Collection<Coordinate> coordinates) {
        CurveString cs = new CurveString();

        Iterator<Coordinate> it = coordinates.iterator();
        Coordinate prev = it.next();
        while (it.hasNext()) {
            Coordinate cur = it.next();
            cs.curves.add(new Line(prev,cur));
            prev =cur;
        }
        return cs;
    }
    public static CurveString fromCoordinatesLinear(Coordinate[] coordinates) {
        CurveString cs = new CurveString();

        for (int i = 1; i < coordinates.length-1; ++i) {
            cs.curves.add(new Line(coordinates[i-1],coordinates[i]));
        }

        return cs;
    }

    public static CurveString fromCoordinates(ArrayList<Coordinate> coordinates) {
        return fromCoordinates(coordinates.toArray(new Coordinate[coordinates.size()]));
    }

    public static CurveString fromOcadVertices(OcadVertex[] poly ) throws Exception {
        int i = 0;
        CurveString cs = new CurveString();
        while (i < poly.length-1) {
            Curve c = null;
            if (i+3 < poly.length && (c=BezierQuadraticCurve.fromOcadVertices(poly[i],poly[i+1],poly[i+2],poly[i+3]))!=null) {
                if (cs == null || cs.curves == null) {
                    cs.curves.add(c);
                }
                cs.curves.add(c);
                i += 3;
            } else if (i+2 < poly.length && (c=BezierCubicCurve.fromOcadVertices(poly[i],poly[i+1],poly[i+2]))!=null) {
                cs.curves.add(c);
                i+=2;
            } else if (i+1 < poly.length && (c=Line.fromOcadVertices(poly[i],poly[i+1]))!=null) {
                cs.curves.add(c);
                i+=1;
            } else {
                i += 1;
                throw new Exception("Invalid poly array");
            }
        }
        return cs;
    }

    public static CurveString fromOcadVertices(Collection<OcadVertex> polyCollection) throws Exception {
        //Conver to ArrayList for random access
        return fromOcadVertices(polyCollection.toArray( new OcadVertex[polyCollection.size()]) );
    }
}
