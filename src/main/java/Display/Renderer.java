package Display;

import com.sun.org.apache.bcel.internal.generic.SWAP;
import com.vividsolutions.jts.geom.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * Created by Artyom.Fomenko on 15.07.2016.
 * Renders various geometry
 */
public class Renderer {

    List<GeometryWrapper> gws;

    private Coordinate center;
    private double scale;

    public Coordinate getCenter() {
        return new Coordinate(center);
    }

    public void setCenter(double x, double y) {
        center.x = x;
        center.y = y;
    }


    public void setCenter(Coordinate c) {
        center.x = c.x;
        center.y = c.y;
    }

    /**
     * Rescale viewport with pivot as center
     * @param pivot
     * @param scale_delta
     * @return
     */
    public void rescale(Coordinate pivot, double scale_delta) {
        center.x-=pivot.x;
        center.y-=pivot.y;
        scale*=scale_delta;
        center.x*=scale_delta;
        center.y*=scale_delta;
        center.x+=pivot.x;
        center.y+=pivot.y;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double s) {
        scale = s;
    }

    public Renderer() {
        gws = new ArrayList<GeometryWrapper>();
        center = new Coordinate(0,0);
    }

    public void add (GeometryWrapper gw) {gws.add(gw);};

    public void addAll(Collection<? extends GeometryWrapper> geom) {
        gws.addAll(geom);
    }

    public void clear() {
        gws.clear();
    }


    public void screenToLocal(Coordinate c, double screenWidth, double screenHeight) {
        double correction_scale = (Math.min(screenWidth,screenHeight));
        Coordinate correction_shift = new Coordinate(screenWidth*0.5,screenHeight*0.5);
        c.y = screenHeight-c.y;
        Transform(c, correction_shift, correction_scale);
        ReverseTransform(c,center,scale);
    }

    public void localToScreen(Coordinate c, double screenWidth, double screenHeight) {
        double correction_scale = (Math.min(screenWidth,screenHeight));
        Coordinate correction_shift = new Coordinate(screenWidth*0.5,screenHeight*0.5);
        Transform(c,center,scale);
        ReverseTransform(c,correction_shift,correction_scale);
        c.y = screenHeight-c.y;
    }

    /**
     * Transform point's x and y to range -1,1 (onscreen coordinates)
     * @param c
     * @param center
     * @param scale
     */
    private void Transform(Coordinate c,Coordinate center,double scale) {
        c.x = (c.x-center.x)/scale;
        c.y = (c.y-center.y)/scale;
    }

    /**
     * Transform point's x and y from -1,1 (onscreen coordinates) to local
     * @param c
     * @param center
     * @param scale
     */
    private void ReverseTransform(Coordinate c, Coordinate center,double scale) {
        c.x = (c.x*scale)+center.x;
        c.y = (c.y*scale)+center.y;
    }

    public void Fit() {

        Envelope bbox = new Envelope();
        for (GeometryWrapper g : gws) {
            bbox.expandToInclude(g.geom.getEnvelopeInternal());
        }

        if (bbox.centre() != null) {
            this.setCenter(bbox.centre());
        }
        this.scale = (double)(Math.max(bbox.getWidth(),bbox.getHeight()));;
    }

    public void render(List<GeometryWrapper> gws, GraphicsContext gc, double Width, double Height) {
        double correction_scale = (double)(Math.min(Width,Height));
        Coordinate correction_shift = new Coordinate(Width*0.5,Height*0.5);
        for (GeometryWrapper gs : gws) {

            gc.setLineWidth(gs.width);
            gc.setStroke(gs.color);

            Geometry geometry = gs.geom;
            LineString string = null;
            if (geometry instanceof LineString) string = (LineString) geometry;
            if (geometry instanceof Polygon) string = ((Polygon)geometry).getExteriorRing();
            if (geometry instanceof Point) {
                continue;
            }
            if (string == null) continue;

            CoordinateSequence coords = string.getCoordinateSequence();

            Coordinate c1 = new Coordinate(coords.getCoordinate(0));
            Transform(c1,center,scale);
            ReverseTransform(c1,correction_shift,correction_scale);
            Coordinate c2 = new Coordinate();

            if (gs.geom.getNumPoints() == 1) {
                gc.setFill(gs.color);
                gc.fillOval(c1.x,Height-c1.y,gs.width,gs.width);
                continue;
            }

            for (int i = 1; i < coords.size(); ++i) {
                c2.x = coords.getX(i);
                c2.y = coords.getY(i);
                Transform(c2,center,scale);
                ReverseTransform(c2,correction_shift,correction_scale);
                gc.strokeLine(c1.x, Height-c1.y, c2.x, Height-c2.y);
                c1.x = c2.x;
                c1.y = c2.y;
            }
        }
    }

    public void render(GraphicsContext gc, double Width, double Height) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,Width,Height);
        render(gws,gc, Width, Height);
    }

    public void render(Geometry geometry, GraphicsContext gc, double Width, double Height) {

        double correction_scale = (double)(Math.min(Width,Height));
        Coordinate correction_shift = new Coordinate(Width*0.5,Height*0.5);

        LineString string = null;
        if (geometry instanceof LineString) string = (LineString) geometry;
        if (geometry instanceof Polygon) string = ((Polygon)geometry).getExteriorRing();
        if (geometry instanceof Point) {
            return;
        }
        if (string == null) return;

        CoordinateSequence coords = string.getCoordinateSequence();

        Coordinate c1 = new Coordinate(coords.getCoordinate(0));
        Transform(c1,center,scale);
        ReverseTransform(c1,correction_shift,correction_scale);
        Coordinate c2 = new Coordinate();

        for (int i = 1; i < coords.size(); ++i) {
            c2.x = coords.getX(i);
            c2.y = coords.getY(i);
            Transform(c2,center,scale);
            ReverseTransform(c2,correction_shift,correction_scale);
            gc.strokeLine(c1.x, Height-c1.y, c2.x, Height-c2.y);
            c1.x = c2.x;
            c1.y = c2.y;
        }
    }

}
