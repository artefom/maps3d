package ru.ogpscenter.maps3d.display;

import Deserialization.Interpolation.SlopeMark;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import javafx.scene.paint.Color;
import ru.ogpscenter.maps3d.algorithm.repair.MapEdge;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;
import ru.ogpscenter.maps3d.isolines.SlopeSide;
import ru.ogpscenter.maps3d.utils.Constants;
import ru.ogpscenter.maps3d.utils.LineStringInterpolatedLineIterator;
import ru.ogpscenter.maps3d.utils.LineStringInterpolatedPointIterator;

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
    public List<GeometryWrapper> draw(IsolineContainer isolines, MapEdge edge) {
        ArrayList<GeometryWrapper> geom = new ArrayList<>();

        for (IIsoline i: isolines) {

            LineString ls = i.getLineString();

            Color col = i.getLineString().isClosed() ?
                    Constants.DRAWING_COLOR_ISOLINE_CLOSED:
                    Constants.DRAWING_COLOR_ISOLINE;

            if (i.isEdgeToEdge()){
                col = Constants.DRAWING_COLOR_EGDE_TO_EDGE;
            }

            if (i.getSlopeSide() != SlopeSide.NONE) {
                LineSegment buf = new LineSegment();
                Iterator<LineSegment> it = new LineStringInterpolatedLineIterator(i.getLineString(), buf, 50,0.5);
                while (it.hasNext()) {
                    buf = it.next();
                    Vector2D vec = Vector2D.create(buf.p0, buf.p1).normalize().rotateByQuarterCircle(1).
                            multiply(Constants.slope_length * i.getSlopeSide().getIntValue());
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
            LineString outerRectangle = edge.outerRectangle;
            geom.add( new GeometryWrapper(outerRectangle, Constants.DRAWING_COLOR_BORDER,1));
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

    public List<GeometryWrapper> draw(IIsoline i, Color color, double width) {

        ArrayList<GeometryWrapper> geom = new ArrayList<>();

        LineString ls = i.getLineString();

        Color col = color;
        if (col == null) {
            col = i.getLineString().isClosed() ?
                    Constants.DRAWING_COLOR_ISOLINE_CLOSED :
                    Constants.DRAWING_COLOR_ISOLINE;

            if (i.isEdgeToEdge()) {
                col = Constants.DRAWING_COLOR_EGDE_TO_EDGE;
            }
        }

        if (i.getSlopeSide() != SlopeSide.NONE) {
            LineSegment buf = new LineSegment();
            Iterator<LineSegment> it = new LineStringInterpolatedLineIterator(i.getLineString(), buf, 50,0.5);
            while (it.hasNext()) {
                buf = it.next();
                Vector2D vec = Vector2D.create(buf.p0, buf.p1).normalize().rotateByQuarterCircle(1).
                        multiply(Constants.slope_length*i.getSlopeSide().getIntValue());
                Coordinate p0 = buf.midPoint();
                Coordinate p = vec.add(Vector2D.create(p0)).toCoordinate();
                LineString slope_ls = gf.createLineString(new Coordinate[]{p0, p});
                geom.add(new GeometryWrapper(slope_ls, col, 1*width));
            }
        }

        geom.add( new GeometryWrapper( ls, col, i.getType()*Constants.DRAWING_LINE_WIDTH*width ));
        double d = (double)i.getType()*Constants.DRAWING_POINT_WIDTH;
        geom.add( new GeometryWrapper( gf.createPoint(ls.getCoordinateN(0)), col, d*width ));

        return geom;
    }

}
