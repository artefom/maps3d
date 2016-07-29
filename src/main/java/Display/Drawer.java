package Display;

import Algorithm.EdgeDetection.Edge;
import Isolines.IIsoline;
import Isolines.Isoline;
import Isolines.IsolineContainer;
import Loader.Interpolation.SlopeMark;
import Utils.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * Created by Artem on 17.07.2016.
 * Class translates isoline container to drawing primitives, which are then processed
 * by renderer
 */
public class Drawer {

    GeometryFactory gf;
    public Drawer( GeometryFactory gf ) {
        this.gf = gf;
    }

    /**
     * Convers IIsoline Container to geometry
     * @param isolines
     * @return
     */
    public List<GeometryWrapper> draw(IsolineContainer isolines, Edge edge) {
        ArrayList<GeometryWrapper> geom = new ArrayList<>();

        for (IIsoline i: isolines) {

            LineString ls = i.getLineString();



            if (Constants.DRAWING_INTERPOLATION)
                ls = interpolatedLine(ls,Constants.DRAWING_INTERPOLATION_STEP);
            Color col = i.getLineString().isClosed() ?
                    Constants.DRAWING_COLOR_ISOLINE_CLOSED:
                    Constants.DRAWING_COLOR_ISOLINE;

            if (i.isEdgeToEdge()){
                col = Constants.DRAWING_COLOR_EGDE_TO_EDGE;
            }

            if (i.getSlopeSide() != 0) {
                LineSegment buf = new LineSegment();
                Iterator<LineSegment> it = new LineStringInterpolatedLineIterator(i.getLineString(), buf, 5,0.5);
                while (it.hasNext()) {
                    buf = it.next();
                    Vector2D vec = Vector2D.create(buf.p0, buf.p1).normalize().rotateByQuarterCircle(1).
                            multiply(Constants.slope_length*i.getSlopeSide());
                    Coordinate p0 = buf.midPoint();
                    Coordinate p = vec.add(Vector2D.create(p0)).toCoordinate();
                    LineString slope_ls = gf.createLineString(new Coordinate[]{p0, p});
                    geom.add(new GeometryWrapper(slope_ls, col, 1));
                }
            }

            geom.add( new GeometryWrapper( ls, col, i.getType()*Constants.DRAWING_LINE_WIDTH ));
            double d = (double)i.getType()*Constants.DRAWING_POINT_WIDTH;
            geom.add( new GeometryWrapper( gf.createPoint(ls.getCoordinateN(0)), col, d ));
        }

        if (edge != null) {
            LineString ls = edge.outerBound;
            geom.add( new GeometryWrapper(ls, Constants.DRAWING_COLOR_CONCAVEHULL,1) );
        }

        return geom;
    }

    public List<GeometryWrapper> drawGeometry(Collection<LineString> geometries, Color color) {
      List<GeometryWrapper> gws = new ArrayList<>();
        for (Geometry g : geometries) {
            gws.add(new GeometryWrapper(g,color,1));
        }
        return gws;
    }

    public List<GeometryWrapper> draw(Collection<SlopeMark> slopes) {
        ArrayList<GeometryWrapper> gws = new ArrayList<>();
        slopes.forEach((s)->{
            gws.add(new GeometryWrapper( s.asGeometry(Constants.slope_length,gf), Constants.DRAWING_COLOR_SLOPE_ORIGINAL, 1 ));
        });
        return gws;
    }

    private LineString interpolatedLine(LineString ls, double step) {
        LineStringInterpolatedPointIterator it = new LineStringInterpolatedPointIterator(ls,step,0);

        LinkedList<Coordinate> coords_list = new LinkedList<>();
        int i = 0;
        while (it.hasNext()) {
            Coordinate c = it.next();
            coords_list.add(c);
            i += 1;
        }
        return gf.createLineString(coords_list.toArray(new Coordinate[coords_list.size()]));
    }

    // Debug method
    public List<GeometryWrapper> drawTraces(Collection<Geometry> occluders, LineString ls) {
        List<GeometryWrapper> gws = new ArrayList<>();

        Tracer<Geometry> tracer = new Tracer<>(occluders,(iso)->iso,gf);
        LineSegment buf = new LineSegment();

        LineStringInterpolatedLineIterator it = new LineStringInterpolatedLineIterator(ls,
                buf, Constants.NEARBY_TRACE_STEP,Constants.NEARBY_TRACE_STEP*0.01);
        while (it.hasNext()) {
            it.next();
            Coordinate trace_base = buf.midPoint();
            Vector2D trace_positive_vec = Vector2D.create(buf.p0,buf.p1).rotateByQuarterCircle(1).normalize();
            Vector2D trace_negative_vec = Vector2D.create(trace_positive_vec).negate();
            Tracer.traceres traced_positive_pair =
                    tracer.trace(trace_base,trace_positive_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);
            Tracer.traceres traced_negetive_pair =
                    tracer.trace(trace_base,trace_negative_vec, Constants.NEARBY_TRACE_OFFSET,Constants.NEARBY_TRACE_LENGTH);

            if (traced_positive_pair.entitiy != null)
                gws.add( new GeometryWrapper( gf.createLineString(new Coordinate[]{trace_base,traced_positive_pair.point}),Color.RED,1) );
            if (traced_negetive_pair.entitiy != null)
                gws.add( new GeometryWrapper( gf.createLineString(new Coordinate[]{trace_base,traced_negetive_pair.point}),Color.BLUE,1) );
        }
        return gws;
    }

    public List<GeometryWrapper> draw(IIsoline i, Color color, double width) {
        List<GeometryWrapper> gws = new ArrayList<>();
        gws.add(new GeometryWrapper(i.getLineString(),color,width));
        return gws;
    }

}
