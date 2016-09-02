package Utils.Area;

import Utils.GeomUtils;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Artyom.Fomenko on 23.08.2016.
 */
public class PointAreaBuffer<T> extends AreaBuffer< CoordinateAttributed<T> > {

    @Override
    public boolean add(CoordinateAttributed<T> entity) {
        int local_x = (int)toLocalX(entity.x);
        int local_y = (int)toLocalY(entity.y);
        if (local_x < 0 || local_x > width || local_y < 0 || local_y > height) {
            return false;
        }
        putToCell(local_x, local_y, entity);
        return true;
    }

    public boolean add( Coordinate c ) {
        int local_x = (int)toLocalX(c.x);
        int local_y = (int)toLocalY(c.y);
        if (local_x < 0 || local_x >= width || local_y < 0 || local_y >= height) {
            return false;
        }
        putToCell(local_x,local_y,new CoordinateAttributed<T>(c,null));
        return true;
    }

    @Override
    public boolean remove(CoordinateAttributed<T> entity) {
        int local_x = (int)toLocalX(entity.x);
        int local_y = (int)toLocalY(entity.y);
        if (local_x < 0 || local_x >= width || local_y < 0 || local_y >= height) {
            return false;
        }
        return getCell(local_x,local_y).remove(entity);
    }

    @Override
    public void setEnvelope(Collection<CoordinateAttributed<T>> entities) {
        Iterator<CoordinateAttributed<T>> it = entities.iterator();
        if (!it.hasNext()) throw new RuntimeException("Entites collection must not be empty!");

        CoordinateAttributed<T> c = it.next();

        double x = c.x;
        double y = c.y;
        setEnvelope(x,x,y,y);

        while (it.hasNext()) {

            c = it.next();

            x = c.x;
            y = c.y;
            expandEnvelopeToInclude(x,y);

        }
    }

    private ArrayList<CoordinateAttributed<T>> find_buf = new ArrayList<>();
    public void findInRadius(double x0, double y0, double r, Collection<CoordinateAttributed<T>> ret ) {;
        find_buf.clear();
        findInArea(x0,y0,r,find_buf);
        for (CoordinateAttributed<T> c : find_buf) {
            double vx = c.x-x0;
            double vy = c.y-y0;
            if ( Math.sqrt(vx*vx+vy*vy) <= r ) ret.add(c);
        }
    }

    public void findInRadius(Coordinate c, double r, Collection<CoordinateAttributed<T>> ret) {
        findInRadius(c.x,c.y,r,ret);
    }

    public int countInRadius(double x0, double y0, double r) {
        int ret = 0;
        find_buf.clear();
        findInArea(x0,y0,r,find_buf);
        for (CoordinateAttributed<T> c : find_buf) {
            double vx = c.x-x0;
            double vy = c.y-y0;
            if ( Math.sqrt(vx*vx+vy*vy) <= r ) ret += 1;
        }
        return ret;
    }

    public int countInRadius(Coordinate c, double r) {
        return countInRadius(c.x,c.y,r);
    }


    public boolean hasInRadius(double x0, double y0, double r) {
        if (!initialized) throw new RuntimeException("Buffer not initialized!");

        int begin_x =   (int)GeomUtils.clamp(toLocalX(x0-r),0,width-1);
        int end_x   =   (int)GeomUtils.clamp(toLocalX(x0+r),0,width-1);
        int begin_y =   (int)GeomUtils.clamp(toLocalY(y0-r),0,height-1);
        int end_y   =   (int)GeomUtils.clamp(toLocalY(y0+r),0,height-1);

        for (int x = begin_x; x <= end_x; ++x) {
            for (int y = begin_y; y <= end_y; ++y) {
                for ( CoordinateAttributed<T> c : getCell(x,y)) {
                    double vx = c.x-x0;
                    double vy = c.y-y0;
                    if ( Math.sqrt(vx*vx+vy*vy) <= r ) return true;
                }
            }
        }

        return false;
    }


}
