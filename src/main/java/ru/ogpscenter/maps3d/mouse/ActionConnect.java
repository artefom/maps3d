package ru.ogpscenter.maps3d.mouse;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import ru.ogpscenter.maps3d.algorithm.repair.AttributedIsoline;
import ru.ogpscenter.maps3d.algorithm.repair.Connection;
import ru.ogpscenter.maps3d.algorithm.repair.LineConnector;
import ru.ogpscenter.maps3d.algorithm.repair.LineEnd;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.Isoline;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;
import ru.ogpscenter.maps3d.isolines.SlopeSide;
import ru.ogpscenter.maps3d.utils.Pair;

import java.util.ArrayList;

/**
 * Created by Artem on 21.08.2016.
 */
public class ActionConnect extends ActionBase {

    @Override
    public void execute(Coordinate[] actionPoints, IsolineContainer cont, double near_threshold) throws Exception {
        if (actionPoints.length != 2) throw new Exception("number of action points must be 2");



        Coordinate p0= actionPoints[0];
        Coordinate p1 = actionPoints[1];


        ArrayList<AttributedIsoline> cont_attr = new ArrayList<>();
        for (IIsoline iso : cont) {
            cont_attr.add(new AttributedIsoline(iso));
        }

        ArrayList<Pair<LineEnd,Double>> closest_lineEnds = new ArrayList<>();

        // Find first closest end;
        for (AttributedIsoline iso : cont_attr) {

            LineEnd le0 = LineEnd.fromIsoline(iso,1);
            LineEnd le1 = LineEnd.fromIsoline(iso,-1);

            if (le0 != null) {
                closest_lineEnds.add(new Pair<LineEnd, Double>(le0, le0.line.p1.distance(p0)));
                closest_lineEnds.add(new Pair<LineEnd, Double>(le0, le0.line.p1.distance(p1)));
            }
            if (le1 != null) {
                closest_lineEnds.add(new Pair<LineEnd, Double>(le1, le1.line.p1.distance(p0)));
                closest_lineEnds.add(new Pair<LineEnd, Double>(le1, le1.line.p1.distance(p1)));
            }
            closest_lineEnds.sort((lhs,rhs)-> Double.compare(lhs.v2,rhs.v2));
            while (closest_lineEnds.size() > 2) closest_lineEnds.remove(closest_lineEnds.size()-1);
        }

        if (closest_lineEnds.size() < 2 || closest_lineEnds.get(1).v2 > near_threshold*3) throw new Exception("No line ends found");

        LineEnd le0 = closest_lineEnds.get(0).v1;
        LineEnd le1  = closest_lineEnds.get(1).v1;


        if (le0 == null || le1 == null) throw new Exception("Invalid line ends");

        if (le0.isoline.getIsoline().getType() != le1.isoline.getIsoline().getType()) throw new Exception("Line type does not match");

        if (le0.isoline.getIsoline().getHeight() != le1.isoline.getIsoline().getHeight()) throw new Exception("Line height does not match");

        Connection con = Connection.fromLineEnds(le0,le1);

        if (!con.isValid()) throw new Exception("Invalid connection");

        Pair<LineString, SlopeSide> pair = LineConnector.connect(con,cont.getFactory(),false);

        if (pair == null || pair.v1 == null) throw new Exception("Connection operation failed");

        IIsoline new_iso = new Isoline(le0.isoline.getIsoline().getType(),pair.v2,pair.v1.getCoordinateSequence(),cont.getFactory());

        new_iso.setHeight(le0.isoline.getIsoline().getHeight());

        cont.remove(le0.isoline.getIsoline());
        cont.remove(le1.isoline.getIsoline());
        cont.add(new_iso);
    }
    @Override
    public int essentialCoordinates() {
        return 2;
    }

    @Override
    public int maxCooordinates() {
        return 2;
    }
}
