package mouse;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;

/**
 * Base class, exposing execution interface and container storage
 */
public abstract class ActionBase {

    public abstract void execute(Coordinate[] actionPoints, IsolineContainer cont, double near_threshold) throws Exception;

    public int essentialCoordinates() {return  1;}

    public int maxCooordinates() {return -1;}

}
