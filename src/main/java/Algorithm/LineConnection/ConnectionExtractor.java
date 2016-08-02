package Algorithm.LineConnection;

import Utils.Constants;
import Utils.Intersector;
import Utils.Tracer;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Created by Artem on 20.07.2016.
 * Extracts valid connections from line ends.
 */
public class ConnectionExtractor implements Function<ArrayList<Isoline_attributed>,ArrayList<Connection>> {

    private Intersector intersector;
    private GeometryFactory gf;
    private ConnectionEvaluator evaluator;
    private SteepDetector steepDetector;
    private MapEdge edge;

    public ConnectionExtractor(Intersector intersector, SteepDetector steepDetector, ConnectionEvaluator evaluator,
                               GeometryFactory gf,MapEdge edge) {
        this.intersector = intersector;
        this.edge = edge;
        this.gf = gf;
        this.evaluator = evaluator;
        this.steepDetector = steepDetector;
        buffer = new Connection();
    }

    private Connection buffer;

    /**
     * Main logics for line connection. Alter cooficients here, add new tests, etc.
     * @param cons
     * @param le1
     * @param le2
     */
    private void addIfNotIntersects(ArrayList<Connection> cons, LineEnd le1, LineEnd le2) {
        if (le1 == null || le2 == null) return;
        buffer.SetLineEnds(le1,le2);
        //if (edge.isWithinEdge(le1.line.p1) || edge.isWithinEdge(le2.line.p1)) return;
        buffer.score = evaluator.apply(buffer);
        if (buffer.score > -0.5) {
            if (!intersector.intersects(buffer.getConnectionSegment()) || buffer.connectionSegment.getLength() < 0.001) {
                if ( (buffer.connectionSegment.getLength() < Constants.CONNECTIONS_WELD_DIST ) ||
                        !steepDetector.isNearSteep(buffer)) {
                    buffer.score += ConnectionEvaluator.parallelScore(buffer) / 2;

                    if (buffer.first().isWithinEdge(edge))
                        buffer.score -= 0.7;
                    if (buffer.second().isWithinEdge(edge))
                        buffer.score -= 0.7;
                    for (Connection con : cons) {
                        if (Tracer.intersects(buffer.getConnectionSegment(),con.getConnectionSegment(),0.01,0.99)) {
                            buffer.score *= 0.5;
                            con.score *= 0.5;
                        }
                    }
                    cons.add(buffer);
                    buffer = new Connection();
                }
            }
        }
    }

    @Override
    public ArrayList<Connection> apply(ArrayList<Isoline_attributed> isolines) {

        // pre-return container. Remove all conflicting(intersecting) connections before return
        ArrayList<Connection> pre_ret = new ArrayList<>(isolines.size()*4);
        Intersector self_intersector;

        for (int i = 0; i != isolines.size(); ++i) {
            Isoline_attributed i1 = isolines.get(i);

            // Add connection line to itself
            addIfNotIntersects(pre_ret, i1.begin,i1.end );

            for (int j = i+1; j < isolines.size(); ++j) {
                Isoline_attributed i2 = isolines.get(j);

                if (i2.getType() == i1.getType()) {
                    addIfNotIntersects(pre_ret, i1.begin, i2.begin);
                    addIfNotIntersects(pre_ret, i1.begin, i2.end);
                    addIfNotIntersects(pre_ret, i1.end, i2.begin);
                    addIfNotIntersects(pre_ret, i1.end, i2.end);
                }
            }
        }

        pre_ret.sort((lhs,rhs)->Double.compare(rhs.score,lhs.score));

        ArrayList<Connection> ret = new ArrayList<>(pre_ret.size());
        for (Connection con1 : pre_ret) {
            // Check for intersection with any of already added connections
            boolean intersected = false;
            for (Connection con2 : ret) {
                // Make padding 0.01 and 0.99 to avoid detecting intersection, when two lines intersect at ends.
                if (Tracer.intersects(con1.getConnectionSegment(),con2.getConnectionSegment(),0.01,0.99)) {
                    intersected = true;
                    break;
                }
            }
            if (!intersected) {
                ret.add(con1);
            }
        }

        return ret;
    }

}
