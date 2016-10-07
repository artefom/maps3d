package Algorithm.BuildingIndex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.util.function.Consumer;

/**
 * Created by fdl on 8/5/16.
 */
public class Box {
    private static GeometryFactory gf = new GeometryFactory();
    public double x0, z0, x1, z1;
    private Geometry rectJTS;
    private boolean needsAcceptation = false;

    public Box(){
        x0 = Double.MAX_VALUE; x1 = Double.MIN_VALUE; z0 = Double.MAX_VALUE; z1 = Double.MIN_VALUE;
    }

    public Box(double x0, double z0, double x1, double z1) {
        Coordinate start = new Coordinate(x1, z1);
        rectJTS = gf.createPolygon(new Coordinate[]{start, new Coordinate(x0,z1), new Coordinate(x0,z0), new Coordinate(x1,z0), start});
        this.x0 = x0;
        this.x1 = x1;
        this.z0 = z0;
        this.z1 = z1;
        assert x0 <= x1 && z0 <= z1 : "new Box() contract check failed: " + this.toString();
    }

    private static Coordinate transform(Consumer<Coordinate> transformation, double x, double z) {
        Coordinate c = new Coordinate(x, z);
        transformation.accept(c);
        return c;
    }

    void apply(Consumer<Coordinate> transformation){
        Coordinate c0 = transform(transformation, x0, z0);
        x0 = c0.x;
        z0 = c0.y;
        Coordinate c1 = transform(transformation, x1, z1);
        x1 = c1.x;
        z1 = c1.y;
        acceptUpdates();
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

    public void update(double x, double z){
        needsAcceptation = true;
        x0 = Math.min(x0, x);
        x1 = Math.max(x1, x);
        z0 = Math.min(z0, z);
        z1 = Math.max(z1, z);
        if (Double.isNaN(x + z)) {
            System.err.print("ga");
        }
    }

    public void acceptUpdates() {
        needsAcceptation = false;
        if (x0 > x1) {
            double tmp = x0;
            x0 = x1;
            x1 = tmp;
        }
        if (z0 > z1) {
            double tmp = z0;
            z0 = z1;
            z1 = tmp;
        }
        Coordinate start = new Coordinate(x1, z1);
        try {
            rectJTS = gf.createPolygon(new Coordinate[]{start, new Coordinate(x0, z1), new Coordinate(x0, z0), new Coordinate(x1, z0), start});
        } catch (IllegalArgumentException iae) {
            throw iae;
        }
    }

    public boolean intersects(Geometry t){
        assert !needsAcceptation : "acceptUpdates() hadn't been called";
        return rectJTS.intersects(t);
    }

    public double xsize(){
        assert (x1 - x0) > 0;
        return x1 - x0;
    }

    public double zsize(){
        assert (z1 - z0) > 0;
        return z1 - z0;
    }

    public boolean contains(double x, double z){
        return x0 <= x && x <= x1 && z0 <= z && z <= z1;
    }

    @Override
    public String toString(){
        return String.format("{x0:%f,z0:%f,x1:%f,z1:%f}", x0, z0, x1, z1);
//        return x0 + " " + z0 + " " + x1 + " " + z1;
    }
}
