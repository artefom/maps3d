package ru.ogpscenter.maps3d.mouse;

import com.vividsolutions.jts.geom.Coordinate;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;

/**
 * Created by Artem on 21.08.2016.
 */
public class ActionMove extends ActionBase {

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
