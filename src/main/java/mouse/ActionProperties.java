package mouse;

import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artem on 21.08.2016.
 */
public class ActionProperties extends ActionBase {

    @Override
    public void execute(Coordinate[] actionPoints, IsolineContainer cont, double near_threshold) {

    }

    @Override
    public int essentialCoordinates() {
        return super.essentialCoordinates();
    }

    @Override
    public int maxCooordinates() {
        return super.maxCooordinates();
    }
}
