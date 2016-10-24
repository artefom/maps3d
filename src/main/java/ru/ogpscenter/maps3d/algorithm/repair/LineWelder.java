package ru.ogpscenter.maps3d.algorithm.repair;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.Isoline;
import ru.ogpscenter.maps3d.utils.CachedTracer;
import ru.ogpscenter.maps3d.utils.Constants;
import ru.ogpscenter.maps3d.utils.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
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
    public AttributedIsoline Weld_copy(Connection con) {
        Pair<LineString, Integer> pair = LineConnector.connect(con,gf,false);
        if (pair == null) return null;
        LineString result_ls = pair.getKey();
        Integer result_ss = pair.getValue();
        IIsoline new_iline = new Isoline(con.first().isoline.getType(),
                result_ss,
                result_ls.getCoordinateSequence(),gf);
        AttributedIsoline ret = new AttributedIsoline(new_iline);
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
    public AttributedIsoline Weld(Connection con) {
        Pair<LineString, Integer> pair = LineConnector.connect(con,gf,false);

        if (pair == null) return null;
        LineString result_ls = pair.getKey();
        Integer result_ss = pair.getValue();
        IIsoline new_iline = new Isoline(con.first().isoline.getType(),
                result_ss,
                result_ls.getCoordinateSequence(),gf);

        AttributedIsoline ret = new AttributedIsoline(new_iline,con.first().other,con.second().other);

        return ret;
    };

    /**
     * Perform welding operating on all isolines.
     * Connections with higher score are welded first and than welded connections with less score (if they still remain valid (see {@link Connection#isValid()}))
     * @param cont
     * @param progressUpdate
     * @return
     */
    public LinkedList<IIsoline> weldAll(Collection<IIsoline> cont, BiConsumer<Integer, Integer> progressUpdate) {

        // todo(MS): update progress

        ArrayList<AttributedIsoline> isos = new ArrayList<>(cont.size());
        for (IIsoline i : cont)
            isos.add(new AttributedIsoline(i));

//        ArrayList<LineString> steeps = new ArrayList<>();
//        for (IIsoline i: cont) {
//            if (i.getType() == 4){
//                steeps.add(i.getLineString());
//            }
//        }

        CachedTracer<AttributedIsoline> intersector = new CachedTracer<>(isos, AttributedIsoline::getGeometry, gf);
        //Tracer_Legacy<Geometry> legacy_tracer = new Tracer_Legacy<>(isos.stream().map(AttributedIsoline::getGeometry).collect(Collectors.toList()), (x)->x,gf);
        //SteepDetector steepDetector = new SteepDetector(steeps, Constants.CONNECTIONS_NEAR_STEEP_THRESHOLD, gf);

        RandomForestEvaluator rf_eval = new RandomForestEvaluator(intersector);
        //rf_eval.getConnections(isos,gf);
        //ConnectionExtractor extr = new ConnectionExtractor(intersector, steepDetector, eval, gf, edge);

        List<Connection> connections = RandomForestEvaluator.evaluateConnectionsRandomForest( rf_eval.getConnections(isos,gf,true), "forest.txt") ;

        //Sort by descending order
        connections.sort((lhs,rhs)->Double.compare(rhs.score,lhs.score));

        LinkedList<AttributedIsoline> welded_lines = connections.stream()
//            .filter(con -> con.score > RandomForestEvaluator.finalSocre95PercentPrecisionThreshold)
            .map(this::Weld).collect(Collectors.toCollection(LinkedList::new));

        LinkedList<IIsoline> result = new LinkedList<>();
        setEdgeToEdge(isos, result);
        setEdgeToEdge(welded_lines, result);
        return result;
    }

    private void setEdgeToEdge(Collection<AttributedIsoline> isolines, LinkedList<IIsoline> result) {
        isolines.stream().filter(isoline -> isoline != null && isoline.isValid()).forEach(validIsoline -> {
            IIsoline iline = validIsoline.getIsoline();
            if (iline != null) {
                iline.setEdgeToEdge(validIsoline.isEdgeToEdge(edge));
                result.add(iline);
            }
        });
    }

}
