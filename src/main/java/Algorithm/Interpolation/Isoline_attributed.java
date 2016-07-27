package Algorithm.Interpolation;

import Isolines.IIsoline;
import Utils.GeomUtils;
import Utils.Tracer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class Isoline_attributed {

    private IIsoline isoline;
    double[] distances;
    Isoline_attributed[] isolines;
    int[] indexes;

    public Isoline_attributed(IIsoline isoline) {
        this.isoline = isoline;
        distances = new double[isoline.getLineString().getNumPoints()];
        isolines = new Isoline_attributed[isoline.getLineString().getNumPoints()];
        indexes = new int[isoline.getLineString().getNumPoints()];
    }

    public void matchIfLess(Isoline_attributed other) {
        Coordinate[] selfCoords = getIsoline().getLineString().getCoordinates();
        Coordinate[] otherCoords = other.getIsoline().getLineString().getCoordinates();
        for (int i = 0; i != distances.length; ++i) {
            Coordinate pivot = selfCoords[i];
            for (int j = 0; j != otherCoords.length; ++j) {
                Coordinate dest = otherCoords[j];
                double dist =  pivot.distance(dest);
                if (dist < distances[i] || isolines[i] == null) {

                    // Check for line side
                    if (i >= 1 && i+1 < selfCoords.length) {
                        Coordinate p1 = selfCoords[i-1];
                        Coordinate p2 = selfCoords[i];
                        Coordinate p3 = selfCoords[i+1];
                        Coordinate ppivot = dest;
                        if (getSide(ppivot,p1,p2,p3) == -1) continue;
                    }

                    // Check for self-intersection
                    if (Tracer.intersects(this.getIsoline().getLineString(),new LineSegment(pivot,dest),0.00001,1))
                        continue;

                    distances[i] = dist;
                    isolines[i] = other;
                    indexes[i] = j;
                }
            }
        }
    }

    public IIsoline getIsoline() {
        return isoline;
    }

    /**
     * Used for displaying connections
     */
    public List<LineString> getMatchingLines(GeometryFactory gf) {
        Coordinate[] selfCoords = getIsoline().getLineString().getCoordinates();
        ArrayList<LineString> ret = new ArrayList<>();
        for (int i = 0; i != distances.length; ++i) {
            if (isolines[i] == null) continue;
            Coordinate pivot = selfCoords[i];
            Coordinate target = isolines[i].getIsoline().getLineString().getCoordinateN(indexes[i]);
            ret.add(gf.createLineString(new Coordinate[]{pivot,target}));
        }
        return ret;
    }

    public static int getSide(Coordinate pivot, Coordinate p1, Coordinate p2, Coordinate p3) {
        LineSegment ls1 = new LineSegment(p1,p2);
        LineSegment ls2 = new LineSegment(p2,p3);
        if (ls1.projectionFactor(pivot) < 1) return GeomUtils.getSide(ls1,pivot);
        return GeomUtils.getSide(ls2,pivot);
    }

}
