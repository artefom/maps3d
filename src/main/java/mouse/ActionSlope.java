package mouse;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.CachedTracer;
import Utils.GeomUtils;
import Utils.LineStringIterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;

import java.util.*;

/**
 * Created by Artem on 21.08.2016.
 */
public class ActionSlope extends  ActionBase {

    @Override
    public void execute(Coordinate[] actionPoints, IsolineContainer cont, double near_threshold) throws Exception {
        if (actionPoints == null || cont == null || actionPoints.length <= 1)
            throw new Exception("Invalid action points or isoline container");

        LineString cut_line = cont.getFactory().createLineString(actionPoints);

        List<IIsoline> intersected_isolines = cont.getIntersecting(cut_line);
        for (IIsoline iso : intersected_isolines) iso.setSlopeSide(0);
        HashSet<IIsoline> positive_side = new HashSet<>();
        HashSet<IIsoline> negative_side = new HashSet<>();

        CachedTracer<IIsoline> tracer = new CachedTracer<>(intersected_isolines,(x)->x.getLineString(),cont.getFactory());
        LineSegment buf = new LineSegment();

        LineStringIterator it = new LineStringIterator(cut_line,buf);;
        while (it.hasNext()) {
            it.next();
            Vector2D vec = Vector2D.create(buf.p0,buf.p1);
            CachedTracer.traceres res = tracer.trace(buf.p0, vec,0,1);
            while (res.entitiy != null) {

                if (-res.side > 0) positive_side.add((IIsoline)res.entitiy);
                else negative_side.add((IIsoline)res.entitiy);

                vec = Vector2D.create(res.point,buf.p1);
                res = tracer.trace(res.point, vec,0.001,1);

            }
        }

        for (IIsoline iso : intersected_isolines) {
            boolean positive = positive_side.contains(iso);
            boolean negative = negative_side.contains(iso);

            if (positive && negative) {
                iso.setSlopeSide(0);
            } else if (positive) {
                iso.setSlopeSide(1);
            } else iso.setSlopeSide(-1);
        }
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
