package ru.ogpscenter.maps3d.utils.area;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artyom.Fomenko on 23.08.2016.
 */
public class CoordinateAttributed<T> extends Coordinate {

    public T entity;

    public CoordinateAttributed(Coordinate c,T entity) {
        super(c);
        this.entity = entity;
    }

    public CoordinateAttributed(double x, double y, T entity) {
        super(x, y);
        this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CoordinateAttributed<?> that = (CoordinateAttributed<?>) o;

        return entity != null ? entity.equals(that.entity) : that.entity == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        return result;
    }

}
