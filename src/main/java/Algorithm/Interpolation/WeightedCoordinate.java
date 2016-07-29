package Algorithm.Interpolation;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artyom.Fomenko on 28.07.2016.
 */
public class WeightedCoordinate extends Coordinate {

    double weight;
    int merged;
    Coordinate as2D;

    public WeightedCoordinate(double x, double y, double z, double weight) {
        super(x, y, z);
        this.weight = weight;
        merged = 1;
        this.as2D = new Coordinate(x,y);
    }


    public WeightedCoordinate(Coordinate c, double weight) {
        super(c);
        this.weight = weight;
        merged = 1;
        this.as2D = new Coordinate(c.x,c.y);
    }

    public WeightedCoordinate(double x, double y, double weight) {
        super(x, y);
        this.weight = weight;
        merged = 1;
        this.as2D = new Coordinate(x,y);
    }

    public void merge(WeightedCoordinate other) {
        double wSelf = ((double)merged)/(merged+other.merged);
        double wOther = ((double)other.merged)/(merged+other.merged);
        this.weight = weight*wSelf + other.weight*wOther;
        this.x = x*wSelf + other.x*wOther;
        this.y = y*wSelf + other.y*wOther;
        this.z = z*wSelf + other.z*wOther;
        this.as2D.x = this.x;
        this.as2D.y = this.y;
        merged += other.merged;
    }

    public Coordinate getAs2D() {
        return as2D;
    }

}
