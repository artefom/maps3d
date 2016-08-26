package Utils.Area;

import Utils.CachedTracer;
import Utils.GeomUtils;
import com.sun.org.apache.xpath.internal.functions.Function2Args;
import com.vividsolutions.jts.geom.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Created by Artyom.Fomenko on 22.08.2016.
 */
public class GeometryAreaBuffer extends AreaBuffer<LineSegmentWrapper> {

    @FunctionalInterface
    private interface Putter{
        void apply(int x, int y);
    }

    private void applyAlong(double x0, double y0, double x1, double y1,Putter pt) {

        int i,n;


        double a, a0,a1,aa,b,d;

        if (x0 < 0 || x0 > width ||
                x1 < 0 || x1 > width ||
                y0 < 0 || y0 > height ||
                y1 < 0 || y1 > height) {

            GeometryFactory gf = new GeometryFactory();
            LineString ls = gf.createLineString(new Coordinate[]{new Coordinate(x0,y0),new Coordinate(x1,y1)});
            Polygon polygon = gf.createPolygon(
                    new Coordinate[]{
                        new Coordinate(0,       0),
                        new Coordinate(width,   0),
                        new Coordinate(width,   height),
                        new Coordinate(0,       height),
                        new Coordinate(0,       0)
                    }
                    );

            Geometry inters = ls.intersection(polygon);
            CoordinateSequence cs = ((LineString)ls.intersection(polygon)).getCoordinateSequence();

            if (cs.size() != 2) return;
            x0 = cs.getX(0);
            y0 = cs.getY(0);
            x1 = cs.getX(1);
            y1 = cs.getY(1);
        }



        a0=Math.ceil(x0);
        a1=Math.floor(x1);
        d=(y1-y0)/(x1-x0);
        a=a0;
        b=y0+(a0-x0)*d;
        n=(int)Math.abs(a1-a0);

        // end-points
        pt.apply((int)x0,(int)y0);
        pt.apply((int)x1,(int)y1);
        // x-axis pixel cross
        a0=1; a1=0; n=0;
        if (x0<x1) { a0=Math.ceil(x0); a1=Math.floor(x1); d=(y1-y0)/(x1-x0); a=a0; b=y0+(a0-x0)*d; n=(int)Math.abs(a1-a0); } else
        if (x0>x1) { a0=Math.ceil(x1); a1=Math.floor(x0); d=(y1-y0)/(x1-x0); a=a0; b=y1+(a0-x1)*d; n=(int)Math.abs(a1-a0); }
        if (a0<=a1) for (aa=a,i=0;i<=n;i++,aa=a,a++,b+=d) { pt.apply((int)aa,(int)b); pt.apply( (int)a,(int)b); }
        // y-axis pixel cross
        a0=1; a1=0; n=0;
        if (y0<y1) { a0=Math.ceil(y0); a1=Math.floor(y1); d=(x1-x0)/(y1-y0); a=a0; b=x0+(a0-y0)*d; n=(int)Math.abs(a1-a0); } else
        if (y0>y1) { a0=Math.ceil(y1); a1=Math.floor(y0); d=(x1-x0)/(y1-y0); a=a0; b=x1+(a0-y1)*d; n=(int)Math.abs(a1-a0); }
        if (a0<=a1) for (aa=a,i=0;i<=n;i++,aa=a,a++,b+=d) { pt.apply((int)b,(int)aa); pt.apply((int)b, (int)a); }
    }

    private void applyAlong(LineSegmentWrapper ls, Putter pt) {
        double x0 = ls.getBeginX();
        double y0 = ls.getBeginY();
        double x1 = ls.getEndX();
        double y1 = ls.getEndY();

        applyAlong(toLocalX(x0),toLocalY(y0),toLocalX(x1),toLocalY(y1),pt);
    }

    @Override
    public boolean add(LineSegmentWrapper lsw) {

        applyAlong(lsw,(x,y)->{
            if (x < 0 || y < 0 || x >= width || y >= height ) return;
            boolean contains = false;
            for (LineSegmentWrapper lsw2 : getCell(x,y)) {
                if (lsw == lsw2) {contains = true; break;}
            }
            if (!contains) putToCell(x,y,lsw);
        });

        return true;
    }

    @Override
    public boolean remove(LineSegmentWrapper entity) {
        applyAlong(entity,(x,y)->{
            if (x < 0 || y < 0 || x >= width || y >= height ) return;
            ArrayList<LineSegmentWrapper> lst = getCell(x,y);
            lst.remove(entity);
        });
        return true;
    }

    @Override
    public void setEnvelope(Collection<LineSegmentWrapper> entities, int width, int height) {


        Iterator<LineSegmentWrapper> it = entities.iterator();
        if (!it.hasNext()) throw new RuntimeException("entities size must be not 0!");
        LineSegmentWrapper lsw = it.next();

        double x = lsw.getBeginX();
        double y = lsw.getBeginY();
        envelope_minX = x;
        envelope_maxX = x;
        envelope_minY = y;
        envelope_maxY = y;

        x = lsw.getEndX();
        y = lsw.getEndY();
        envelope_minX = Math.min(envelope_minX, x);
        envelope_maxX = Math.max(envelope_maxX, x);
        envelope_minY = Math.min(envelope_minY, y);
        envelope_maxY = Math.max(envelope_maxY, y);

        while (it.hasNext()) {
            LineSegmentWrapper ls = it.next();
            x = ls.getBeginX();
            y = ls.getBeginY();
            envelope_minX = Math.min(envelope_minX, x);
            envelope_maxX = Math.max(envelope_maxX, x);
            envelope_minY = Math.min(envelope_minY, y);
            envelope_maxY = Math.max(envelope_maxY, y);

            x = ls.getEndX();
            y = ls.getEndY();
            envelope_minX = Math.min(envelope_minX, x);
            envelope_maxX = Math.max(envelope_maxX, x);
            envelope_minY = Math.min(envelope_minY, y);
            envelope_maxY = Math.max(envelope_maxY, y);
        }

        // Stretch a-bit, so nothing lies on boarder of envelope
        double envelope_width_dilate = (envelope_maxX-envelope_minX)*0.01;
        double envelope_height_dilate = (envelope_maxY-envelope_minY)*0.01;

        if (envelope_width_dilate == 0) envelope_width_dilate = 0.00001;
        if (envelope_height_dilate == 0) envelope_height_dilate = 0.00001;

        envelope_minX-=envelope_width_dilate;
        envelope_maxX+=envelope_width_dilate;
        envelope_minY-=envelope_height_dilate;
        envelope_maxY+=envelope_height_dilate;

        this.width = width;
        this.height = height;

        cells = new ArrayList[width * height];
        for (int i = 0; i != cells.length; ++i) {
            cells[i] = new ArrayList<>(16);
        }
    }

    public void findPossiblyIntersecting(double x0, double y0, double x1, double y1,  Collection<LineSegmentWrapper> ret ) {
        applyAlong(toLocalX(x0),toLocalY(y0),toLocalX(x1),toLocalY(y1),(x,y)->{

            if (x < 0 || y < 0 || x >= width || y >= height ) return;
            for (LineSegmentWrapper w : getCell(x,y)) {
                ret.add(w);
            }
        });
    }

    public void findInBuffer(LineSegment seg, double r, Collection<LineSegmentWrapper> ret) {
        findInBuffer(seg.p0.x,seg.p0.y,seg.p1.x,seg.p1.y,r,ret);
    }

    public void findInBuffer(double x0, double y0, double x1, double y1, double r,Collection<LineSegmentWrapper> ret) {

        int y_r = (int)GeomUtils.clamp(toLocalY(r)+0.5,1,height);
        int x_r = (int)GeomUtils.clamp(toLocalX(r)+0.5,1,width);

        applyAlong(toLocalX(x0),toLocalY(y0),toLocalX(x1),toLocalY(y1),(c_x,c_y)->{

//            if (x < 0 || y < 0 || x >= width || y >= height ) return;


            int begin_x =   GeomUtils.clamp(c_x-x_r,0,width-1);
            int end_x   =   GeomUtils.clamp(c_x+x_r,0,width-1);
            int begin_y =   GeomUtils.clamp(c_y-y_r,0,height-1);
            int end_y   =   GeomUtils.clamp(c_y+y_r,0,height-1);


            for (int x = begin_x; x <= end_x; ++x) {
                for (int y = begin_y; y <= end_y; ++y) {
                    getCell(x,y).forEach(ret::add);
                }
            }

        });

    }

}
