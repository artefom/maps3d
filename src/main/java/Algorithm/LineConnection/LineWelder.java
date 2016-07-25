package Algorithm.LineConnection;

import Algorithm.EdgeDetection.Edge;
import Isolines.IIsoline;
import Isolines.Isoline;
import Utils.Constants;
import Utils.Pair;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Connects lines based on connection priority
 */
public class LineWelder {

    GeometryFactory gf;
    Intersector intersector;
    ConnectionExtractor extr;
    ConnectionEvaluator eval;
    SteepDetector steepDetector;
    Edge edge;

    public LineWelder() {
        this.gf = new GeometryFactory();
        eval = new ConnectionEvaluator(
                Math.toRadians(Constants.CONNECTIONS_MIN_ANGLE_DEG),
                Math.toRadians(Constants.CONNECTIONS_MAX_ANGLE_DEG),
                Constants.CONNECTIONS_MAX_DIST,
                Constants.CONNECTIONS_WELD_DIST
        );
    }

    public LineWelder(GeometryFactory gf, Edge edge) {
        this.edge = edge;
        this.gf = gf;
        eval = new ConnectionEvaluator(
                Math.toRadians(Constants.CONNECTIONS_MIN_ANGLE_DEG),
                Math.toRadians(Constants.CONNECTIONS_MAX_ANGLE_DEG),
                Constants.CONNECTIONS_MAX_DIST,
                Constants.CONNECTIONS_WELD_DIST
                );
    }

    public Isoline_attributed Weld_copy(Connection con) {
        Pair<LineString, Integer> pair = LineConnector.connect(con,gf);
        if (pair == null) return null;
        LineString result_ls = pair.getKey();
        Integer result_ss = pair.getValue();
        IIsoline new_iline = new Isoline(con.first().isoline.getType(),
                result_ss,
                result_ls.getCoordinateSequence(),gf);
        Isoline_attributed ret = new Isoline_attributed(new_iline);
        return ret;
    }

    public Isoline_attributed Weld(Connection con) {
        Pair<LineString, Integer> pair = LineConnector.connect(con,gf);

        if (pair == null) return null;
        LineString result_ls = pair.getKey();
        Integer result_ss = pair.getValue();
        IIsoline new_iline = new Isoline(con.first().isoline.getType(),
                result_ss,
                result_ls.getCoordinateSequence(),gf);

        Isoline_attributed ret = new Isoline_attributed(new_iline,con.first().other,con.second().other);

        return ret;
    };

    public LinkedList<IIsoline> WeldAll(Collection<IIsoline> cont) {
        ArrayList<Isoline_attributed> isos = new ArrayList<>(cont.size());
        for (IIsoline i : cont)
            isos.add(new Isoline_attributed(i));

        ArrayList<LineString> steeps = new ArrayList<>();
        for (IIsoline i: cont) {
            if (i.getType() == 4){
                steeps.add(i.getLineString());
            }
        }

        intersector = new Intersector(isos.stream().map((x)->x.getGeometry()).collect(Collectors.toList()),gf, Constants.CONNECTIONS_INTERSECTION_OFFSET );
        steepDetector = new SteepDetector(steeps, Constants.CONNECTIONS_NEAR_STEEP_THRESHOLD, gf);
        extr = new ConnectionExtractor(intersector,steepDetector,eval,gf,edge);

        ArrayList<Connection> cons_array = extr.apply(isos);


        cons_array.sort((lhs,rhs)->Double.compare(rhs.score,lhs.score));

        LinkedList<Isoline_attributed> welded_lines = new LinkedList<>();
        for (Connection con : cons_array) {
            if (con.score > 0) {
                welded_lines.add( Weld(con) );
            };
        }
        LinkedList<IIsoline> ret = new LinkedList<>();
        for (Isoline_attributed iso: isos)
            if (iso != null && iso.isValid()) {
                IIsoline iline = iso.getIsoline();
                if (iline != null) {
                    iline.setEdgeToEdge(iso.isEdgeToEdge(edge));
                    ret.add(iline);
                }
            }
        for (Isoline_attributed iso: welded_lines)
            if (iso != null && iso.isValid()) {
                IIsoline iline = iso.getIsoline();
                if (iline != null) {
                    iline.setEdgeToEdge(iso.isEdgeToEdge(edge));
                    ret.add(iline);
                }
            }
        return ret;
    }
}
