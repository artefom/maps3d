package Algorithm.BuildingIndex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Created by fdl on 8/5/16.
 */
public class Box {
    private static GeometryFactory gf = new GeometryFactory();
    public final double x0, z0, x1, z1;
    private final Geometry rectJTS;

    public Box(double x0, double z0, double x1, double z1) {
        Coordinate start = new Coordinate(x1, z1);
        rectJTS = gf.createPolygon(new Coordinate[]{start, new Coordinate(x0,z1), new Coordinate(x0,z0), new Coordinate(x1,z0), start});
        this.x0 = x0;
        this.x1 = x1;
        this.z0 = z0;
        this.z1 = z1;
        assert x0 <= x1 && z0 <= z1 : "new Box() contract check failed: " + this.toString();
    }

    public Box[] split(){
        double cx = (x0 + x1)/2, cz = (z0 + z1)/2;
        return new Box[]{
                new Box(cx, cz, x1, z1),
                new Box(x0, cz, cx, z1),
                new Box(x0, z0, cx, cz),
                new Box(cx, z0, x1, cz)
        };
    }

    public boolean intersects(Geometry t){
        return rectJTS.intersects(t);
    }

    public boolean contains(double x, double z){
        return x0 <= x && x <= x1 && z0 <= z && z <= z1;
    }

    @Override
    public String toString(){
        return x0 + " " + z0 + " " + x1 + " " + z1;
    }
}
