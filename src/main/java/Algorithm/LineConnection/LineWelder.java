package Algorithm.LineConnection;

import Isolines.IIsoline;
import Isolines.Isoline;
import Utils.CachedTracer;
import Utils.Constants;
import Utils.Pair;
import Utils.Tracer_Legacy;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Connects lines based on connection priority
 */
public class LineWelder {

    private GeometryFactory gf;
    private ConnectionEvaluator eval;
    private MapEdge edge;

    /**
     * Since {@link LineWelder} avoids connecting line ends situating near map edge, map edge should be passed to it.
     * @param gf used to create new Isolines (performing weld operations)
     * @param edge edge of map
     */
    public LineWelder(GeometryFactory gf, MapEdge edge) {
        this.edge = edge;
        this.gf = gf;
        eval = new ConnectionEvaluator(
                Math.toRadians(Constants.CONNECTIONS_MIN_ANGLE_DEG),
                Math.toRadians(Constants.CONNECTIONS_MAX_ANGLE_DEG),
                Constants.CONNECTIONS_MAX_DIST,
                Constants.CONNECTIONS_WELD_DIST
                );
    }

    /**
     * Used only for test purposes.
     * After perfoming this operation, {@link LineEnd}s of returned isoline ought to be extracted, since none of them does not equal to line ends of connected isolines
     * @param con
     * @return
     */
    public Isoline_attributed Weld_copy(Connection con) {
        Pair<LineString, Integer> pair = LineConnector.connect(con,gf,false);
        if (pair == null) return null;
        LineString result_ls = pair.getKey();
        Integer result_ss = pair.getValue();
        IIsoline new_iline = new Isoline(con.first().isoline.getType(),
                result_ss,
                result_ls.getCoordinateSequence(),gf);
        Isoline_attributed ret = new Isoline_attributed(new_iline);
        return ret;
    }

    /**
     * Performs weld operation
     *
     * WARNING: after two isolines were connected, ther {@link LineEnd}s are not valid. After weld operation, they
     * refer to newformed isoline or to null (if {@link LineEnd}s were ones that being connected by this weld).
     * LineEnds ARE MOVED from old isolines to new one, so all {@link Connection}s remain valid.
     * This is used to avoid re-calculating scores of connections after two isolines were welded
     * @param con
     * @return
     */
    public Isoline_attributed Weld(Connection con) {
        Pair<LineString, Integer> pair = LineConnector.connect(con,gf,false);

        if (pair == null) return null;
        LineString result_ls = pair.getKey();
        Integer result_ss = pair.getValue();
        IIsoline new_iline = new Isoline(con.first().isoline.getType(),
                result_ss,
                result_ls.getCoordinateSequence(),gf);

        Isoline_attributed ret = new Isoline_attributed(new_iline,con.first().other,con.second().other);

        return ret;
    };

    /**
     * Perform welding operating on all isolines.
     * Connections with higher score are welded first and than welded connections with less score (if they still remain valid (see {@link Connection#isValid()}))
     * @param cont
     * @return
     */
    public LinkedList<IIsoline> WeldAll(Collection<IIsoline> cont) {

        ArrayList<Isoline_attributed> isos = new ArrayList<>(cont.size());
        for (IIsoline i : cont)
            isos.add(new Isoline_attributed(i));

//        ArrayList<LineString> steeps = new ArrayList<>();
//        for (IIsoline i: cont) {
//            if (i.getType() == 4){
//                steeps.add(i.getLineString());
//            }
//        }

        CachedTracer<Geometry> intersector = new CachedTracer<>(isos.stream().map(Isoline_attributed::getGeometry).collect(Collectors.toList()),(x)->x, gf);
        //Tracer_Legacy<Geometry> legacy_tracer = new Tracer_Legacy<>(isos.stream().map(Isoline_attributed::getGeometry).collect(Collectors.toList()), (x)->x,gf);
        //SteepDetector steepDetector = new SteepDetector(steeps, Constants.CONNECTIONS_NEAR_STEEP_THRESHOLD, gf);

        RandomForestEvaluator rf_eval = new RandomForestEvaluator(intersector);
        //rf_eval.getConnections(isos,gf);
        //ConnectionExtractor extr = new ConnectionExtractor(intersector, steepDetector, eval, gf, edge);

        ArrayList<Connection> cons_array = RandomForestEvaluator.evaluateConnectionsRandomForest( rf_eval.getConnections(isos,gf), "forest.txt") ;

        //Sort by descending order
        cons_array.sort((lhs,rhs)->Double.compare(rhs.score,lhs.score));

        LinkedList<Isoline_attributed> welded_lines = new LinkedList<>();
        for (Connection con : cons_array) {
            //if (con.score > RandomForestEvaluator.finalSocre95PercentPrecisionThreshold) {
                welded_lines.add( Weld(con) );
            //};
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
