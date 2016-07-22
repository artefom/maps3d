package Algorithm.LineConnection;

import com.vividsolutions.jts.geom.Geometry;

import java.util.List;
import java.util.function.Function;

/**
 * Created by Artem on 20.07.2016.
 * Test geometry for intersection with any of it's content
 */
public class Intersector {

    private List<Geometry> primitives;

    public Intersector( List<Geometry> primitives ) {
        this.primitives = primitives;
    }

    public Boolean apply(Geometry geometry) {
        for (Geometry g : primitives) {
            if (g.intersects(geometry)) return true;
        }
        return false;
    }


}
