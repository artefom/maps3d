package Algorithm.LineConnection;

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

    public ConnectionExtractor(Intersector intersector, ConnectionEvaluator evaluator,
                               GeometryFactory gf) {
        this.intersector = intersector;
        this.gf = gf;
        this.evaluator = evaluator;
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
        buffer.score = evaluator.apply(buffer);
        if (buffer.score > -0.5) {
            if (!intersector.apply(buffer.getConnectionLine()) || buffer.connectionLine.getLength() < 0.001) {

                buffer.score += ConnectionEvaluator.parallelScore(buffer)/2;

                for (Connection con : cons) {
                    if (con.getConnectionLine().intersects(buffer.getConnectionLine())) {
                        buffer.score *= 0.5;
                        con.score *= 0.5;
                    }
                }

                //if (buffer.score > 0.3) {
                    cons.add(buffer);
                    buffer = new Connection();
                //}
            }
        }
    }

    @Override
    public ArrayList<Connection> apply(ArrayList<Isoline_attributed> isolines) {

        ArrayList<Connection> ret = new ArrayList<>(isolines.size()*4);
        Intersector self_intersector;

        for (int i = 0; i != isolines.size(); ++i) {
            Isoline_attributed i1 = isolines.get(i);

            // Add connection line to itself
            addIfNotIntersects(ret, i1.begin,i1.end );

            for (int j = i+1; j < isolines.size(); ++j) {
                Isoline_attributed i2 = isolines.get(j);

                if (i2.getType() == i1.getType()) {
                    addIfNotIntersects(ret, i1.begin, i2.begin);
                    addIfNotIntersects(ret, i1.begin, i2.end);
                    addIfNotIntersects(ret, i1.end, i2.begin);
                    addIfNotIntersects(ret, i1.end, i2.end);
                }
            }
        }

        return ret;
    }

}
