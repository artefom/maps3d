package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.Isoline;
import Utils.LineStringInterpolatedPointIterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class Isoline_attributed {

    public Coordinate[] coordinates;
    private IIsoline isoline;
    private short heightIndex = -1;


    public Isoline_attributed(IIsoline isoline) {
        this.isoline = isoline;
        coordinates = isoline.getLineString().getCoordinates();
    }

    public IIsoline getIsoline() {
        return isoline;
    }

    private List<LineSegment> getMatchingLinesInternal(GeometryFactory gf,
                                                      Isoline_attributed[] isolines,
                                                      int[] indexes) {
        Coordinate[] selfCoords = this.coordinates;
        ArrayList<LineSegment> ret = new ArrayList<>();
        for (int i = 0; i != isolines.length; ++i) {
            if (isolines[i] == null) continue;
            Coordinate pivot = selfCoords[i];
            Coordinate target = isolines[i].coordinates[indexes[i]];
            pivot.z = this.getIsoline().getHeight();
            target.z = isolines[i].getIsoline().getHeight();
            ret.add(new LineSegment(pivot,target));
        }
        return ret;
    }

    public short getHeightIndex() {
        return heightIndex;
    }

    public void setHeightIndex(short heightIndex) {
        this.heightIndex = heightIndex;
    }
}
