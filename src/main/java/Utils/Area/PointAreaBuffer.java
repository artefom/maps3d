package Utils.Area;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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

    @Override
    public boolean remove(CoordinateAttributed<T> entity) {
        int local_x = (int)toLocalX(entity.x);
        int local_y = (int)toLocalY(entity.y);
        if (local_x < 0 || local_x > width || local_y < 0 || local_y > height) {
            return false;
        }
        return getCell(local_x,local_y).remove(entity);
    }

    @Override
    public void setEnvelope(Collection<CoordinateAttributed<T>> entities, int width, int height) {
        Iterator<CoordinateAttributed<T>> it = entities.iterator();
        if (!it.hasNext()) throw new RuntimeException("Entites collection must not be empty!");

        CoordinateAttributed<T> c = it.next();

        double x = c.x;
        double y = c.y;
        envelope_minX = x;
        envelope_maxX = x;
        envelope_minY = y;
        envelope_maxY = y;

        while (it.hasNext()) {

            c = it.next();

            x = c.x;
            y = c.y;
            envelope_minX = Math.min(envelope_minX, x);
            envelope_maxX = Math.max(envelope_maxX, x);
            envelope_minY = Math.min(envelope_minY, y);
            envelope_maxY = Math.max(envelope_maxY, y);

        }

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

}
