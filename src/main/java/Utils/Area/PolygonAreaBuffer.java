package Utils.Area;

import Utils.GeomUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineSegment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Artyom.Fomenko on 04.09.2016.
 */
public class PolygonAreaBuffer extends AreaBuffer<Integer> {

    Coordinate[] coordinates;
    ArrayList<int[]> convex_polygons;

    public PolygonAreaBuffer(Coordinate[] coordinates, ArrayList<int[]> convex_polygons, int width, int height) {
        this.coordinates = coordinates;
        this.convex_polygons = convex_polygons;
        setEnvelope(null);
        init(width,height);
        for (int i = 0; i != convex_polygons.size(); ++i) {
            add(i);
        }
    }

    @Override
    public boolean add(Integer entity_id) {

        int[] entity = convex_polygons.get(entity_id);

        double min_x = coordinates[entity[0]].x;
        double min_y = coordinates[entity[0]].y;
        double max_x = coordinates[entity[0]].x;
        double max_y = coordinates[entity[0]].y;

        for (int i = 1; i < entity.length; ++i) {
            min_x = Math.min(min_x,coordinates[entity[i]].x);
            min_y = Math.min(min_y,coordinates[entity[i]].y);
            max_x = Math.max(max_x,coordinates[entity[i]].x);
            max_y = Math.max(max_y,coordinates[entity[i]].y);
        }

        int begin_x =   GeomUtils.clamp( (int)toLocalX(min_x), 0, width );
        int end_x =     GeomUtils.clamp( (int)toLocalX(max_x)+1, 0, width);
        int begin_y =   GeomUtils.clamp( (int)toLocalY(min_y), 0, height);
        int end_y =     GeomUtils.clamp( (int)toLocalY(max_y)+1, 0, height);


        for (int row = begin_y; row < end_y; ++row) {

            for (int column = begin_x; column < end_x; ++column) {

                putToCell(column,row,entity_id);

            }

        }

        return false;
    }

    @Override
    public boolean remove(Integer entity) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setEnvelope(Collection<Integer> _ignored) {

        Envelope e = new Envelope(coordinates[0]);
        for (Coordinate c : coordinates) {
            e.expandToInclude(c);
        }

        setEnvelope(e.getMinX(),e.getMaxX(),e.getMinY(),e.getMaxY());

    }

    private static LineSegment ls_buf = new LineSegment();
    public boolean contains(int[] poly, Coordinate c) {
        ls_buf.p0 = coordinates[poly[poly.length-1]];
        ls_buf.p1 = coordinates[poly[0]];
        int side = GeomUtils.getSide(ls_buf,c);
        for (int i = 0; i != poly.length-1;++i) {
            ls_buf.p0 = coordinates[poly[i]];
            ls_buf.p1 = coordinates[poly[i+1]];
            int new_side = GeomUtils.getSide(ls_buf,c);
            if (side != 0 && side != GeomUtils.getSide(ls_buf,c)) return false;
            side = new_side;
        }
        return true;
    }

    private static Coordinate coordinate_buf = new Coordinate();
    public int[] getPolygonByPoint(double x, double y) {
        coordinate_buf.x = x;
        coordinate_buf.y = y;
        return getPolygonByPoint(coordinate_buf);
    }

    public int[] getPolygonByPoint(Coordinate c) {
        int column = (int)toLocalX(c.x);
        int row = (int)toLocalY(c.y);

        if (row < 0 || column < 0 || row >= height || column >= width) return null;

        LineSegment ls = new LineSegment();

        int[] closest_poly = null;
        double closest_dist = 0;

        int x_begin = GeomUtils.clamp( column-1, 0, width);
        int y_begin = GeomUtils.clamp( row-1 , 0, height);
        int x_end = GeomUtils.clamp( column+2, 0, width);
        int y_end = GeomUtils.clamp( row+2, 0, height);

        for (int x = x_begin; x != x_end; ++x) {
            for (int y = y_begin; y != y_end; ++y) {
                for ( Integer poly_id : getCell(column,row) ) {
                    int[] poly = convex_polygons.get(poly_id);
                    if (contains(poly,c)) return poly;

                    double dist = Math.min( c.distance(coordinates[poly[0]]), c.distance(coordinates[poly[1]]) );
                    dist = Math.min(dist, c.distance(coordinates[poly[2]]));
                    if (dist < closest_dist || closest_poly == null) {
                        closest_poly = poly;
                        closest_dist = dist;
                    }
                }
            }
        }

        return closest_poly;
    }
}
