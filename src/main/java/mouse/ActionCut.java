package mouse;

import Isolines.IIsoline;
import Isolines.Isoline;
import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import java.util.List;

/**
 * Created by Artem on 21.08.2016.
 */
public class ActionCut extends ActionBase {


    @Override
    public void execute(Coordinate[] actionPoints, IsolineContainer cont, double near_threshold) throws Exception {
        if (actionPoints == null || cont == null || actionPoints.length <= 1)
            throw new Exception("Invalid action points or isoline container");

        LineString cut_line = cont.getFactory().createLineString(actionPoints);

        List<IIsoline> collided_isolines = cont.getIntersecting(cut_line);
        collided_isolines.forEach(cont::remove);

        for (IIsoline iso : collided_isolines) {
            Geometry gc = iso.getLineString().difference(cut_line);
            for (int i = 0; i != gc.getNumGeometries(); ++i) {
                Geometry g = gc.getGeometryN(i);
                if (!(g instanceof LineString)) continue;
                LineString ls = (LineString) g;

                IIsoline iso2 = new Isoline(iso.getType(),iso.getSlopeSide(),ls.getCoordinateSequence(),cont.getFactory());

                cont.add(iso2);
            }
        }

    }

    @Override
    public int essentialCoordinates() {
        return 2;
    }

    @Override
    public int maxCooordinates() {
        return 2;
    }
}
