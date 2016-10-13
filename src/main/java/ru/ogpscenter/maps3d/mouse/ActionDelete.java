package ru.ogpscenter.maps3d.mouse;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;

import java.util.List;

/**
 * Created by Artem on 21.08.2016.
 */
public class ActionDelete extends ActionBase {


    @Override
    public void execute(Coordinate[] actionPoints, IsolineContainer cont, double near_threshold) {
        if (actionPoints == null || cont == null || actionPoints.length == 0) return;
        if (actionPoints.length == 1) {
            deleteSingle(actionPoints[0],cont,near_threshold);
            return;
        }
        deleteIntersecting(cont.getFactory().createLineString(actionPoints),cont);
    }

    private void deleteSingle(Coordinate c, IsolineContainer cont, double near_threshold) {
        List<IIsoline> deletion = cont.findInCircle(c,near_threshold);
        deletion.forEach(cont::remove);
    }

    private void deleteIntersecting(LineString ls, IsolineContainer cont) {
        cont.getIntersecting(ls).forEach(cont::remove);
    }

    @Override
    public int essentialCoordinates() {
        return 1;
    }

}
