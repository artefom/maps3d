package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.Isoline;
import Utils.LineStringInterpolatedPointIterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;

import java.util.ArrayList;
import java.util.List;

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
