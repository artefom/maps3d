package Utils.Area;

import Utils.GeomUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Artyom.Fomenko on 22.08.2016.
 */
public abstract class AreaBuffer<T> {

    public int width;
    public int height;

    public double envelope_minX;
    public double envelope_maxX;
    public double envelope_minY;
    public double envelope_maxY;

    public ArrayList<T>[] cells;


    public AreaBuffer() {

    }

    public final ArrayList<T> getCell(int x, int y) {
        return cells[y*width+x];
    }

    public final void putToCell(int x, int y, T obj) {
        cells[y*width+x].add(obj);
    }

    public final double toLocalX(double x) {
        return GeomUtils.map(x,envelope_minX,envelope_maxX,0,width);
    }

    public final double toLocalY(double y) {
        return GeomUtils.map(y,envelope_minY,envelope_maxY,0,height);
    }

    public final double toGlobalX(double x) {
        return GeomUtils.map(x,0,width,envelope_minX,envelope_maxX);
    }

    public final double toGlobalY(double y) {
        return GeomUtils.map(y,0,height,envelope_minY,envelope_maxY);
    }


    public abstract boolean add(T entity);

    public abstract boolean remove(T entity);

    public final void addAll(Collection<T> entities) {

        for (T ent : entities) {
            add(ent);
        }

    }

    public abstract void setEnvelope(Collection<T> entities, int width, int height);

    public Collection<T> getPossiblyInArea(double c_x, double c_y, double r) {

        HashSet<T> ret = new HashSet<T>();
        int begin_x =   (int)GeomUtils.clamp(toLocalX(c_x-r),0,width-1);
        int end_x   =   (int)GeomUtils.clamp(toLocalX(c_x+r),0,width-1);
        int begin_y =   (int)GeomUtils.clamp(toLocalY(c_y-r),0,height-1);
        int end_y   =   (int)GeomUtils.clamp(toLocalY(c_y+r),0,height-1);

        for (int x = begin_x; x <= end_x; ++x) {
            for (int y = begin_y; y <= end_y; ++y) {
                getCell(x,y).forEach(ret::add);
            }
        }

        return ret;
    }

}
