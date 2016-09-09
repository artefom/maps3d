package Algorithm.Interpolation;

import Isolines.IIsoline;
import com.vividsolutions.jts.geom.CoordinateSequence;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class Isoline_attributed {

    public CoordinateSequence coordinates;
    private IIsoline isoline;
    private short heightIndex = -1;


    public Isoline_attributed(IIsoline isoline) {
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
