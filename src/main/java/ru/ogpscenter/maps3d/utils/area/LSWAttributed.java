package ru.ogpscenter.maps3d.utils.area;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;

/**
 * Created by Artyom.Fomenko on 22.08.2016.
 */
public class LSWAttributed<T> extends LineSegmentWrapper {

    public T entity = null;
    public LSWAttributed(T entity, CoordinateSequence cs, int sement_id) {
        super(cs, sement_id);
        this.entity = entity;
    }

    public static <T> ArrayList<LSWAttributed<T>> fromLineString(T entity, LineString ls) {
        ArrayList<LSWAttributed<T>> ret = new ArrayList<>();
        CoordinateSequence cs = ls.getCoordinateSequence();
        int size = cs.size()-1;
        for (int i = 0; i != size; ++i) {
            ret.add(new LSWAttributed<T>(entity,cs,i));
        }
        return ret;
    }

    public static <T> ArrayList<LSWAttributed<T>> fromPolygon(T entity, Polygon p) {
        ArrayList<LSWAttributed<T>> ret = new ArrayList<>();

        LSWAttributed.fromLineString(entity,p.getExteriorRing()).forEach(ret::add);

        for (int i = 0; i != p.getNumInteriorRing(); ++i) {
            LSWAttributed.fromLineString(entity,p.getInteriorRingN(i)).forEach(ret::add);
        }

        return ret;
    }

    public static <T> ArrayList<LSWAttributed<T>> fromGeometry(T entity, Geometry g_col) {

        ArrayList<LSWAttributed<T>> ret = new ArrayList<>();
        for (int i = 0; i != g_col.getNumGeometries(); ++i) {

            Geometry g = g_col.getGeometryN(i);

            if (LineString.class.isAssignableFrom(g.getClass())) {
                fromLineString(entity,(LineString)g).forEach(ret::add);
            }

            if (Polygon.class.isAssignableFrom(g.getClass())) {
                fromPolygon(entity,(Polygon)g).forEach(ret::add);
            }

        }

        return ret;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LSWAttributed<?> that = (LSWAttributed<?>) o;

        return entity != null ? entity.equals(that.entity) : that.entity == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        return result;
    }

}
