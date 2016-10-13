package ru.ogpscenter.maps3d.algorithm.interpolation;

import com.vividsolutions.jts.geom.CoordinateSequence;
import ru.ogpscenter.maps3d.isolines.IIsoline;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class AttributedIsoline {

    public CoordinateSequence coordinates;
    private IIsoline isoline;
    private short heightIndex = -1;


    public AttributedIsoline(IIsoline isoline) {
        this.isoline = isoline;
        coordinates = isoline.getLineString().getCoordinateSequence();
    }

    public IIsoline getIsoline() {
        return isoline;
    }

    public short getHeightIndex() {
        return heightIndex;
    }

    public void setHeightIndex(short heightIndex) {
        this.heightIndex = heightIndex;
    }
}
