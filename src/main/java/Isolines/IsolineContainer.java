package Isolines;

import Algorithm.LineConnection.LineEnd;
import Utils.GeomUtils;
import Utils.Interpolator;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 * provides container for isolines,
 * high-level geometry operations,
 * isoline indexing
 */
public class IsolineContainer extends HashSet<IIsoline> {

    private GeometryFactory gf;


    public IsolineContainer(GeometryFactory gf, Stream<IIsoline> stream) {
        super((int)stream.count());
        this.gf = gf;
        stream.forEach(this::add);
    }

    public IsolineContainer(GeometryFactory gf, Collection<IIsoline> ilines) {
        super(ilines.size());
        this.gf = gf;
        for ( IIsoline i : ilines ) {
            this.add(i);
        }
    }

    public IsolineContainer(GeometryFactory gf) {
        super();
        this.gf = gf;
    }

    public IsolineContainer(IsolineContainer other) {
        super(other.size());
        this.gf = other.gf;
        other.stream().forEach(this::add);
    }

    /**
     * @return Bounding box of all isolines (eg. usage: fitting view to whole map)
     */
    public Envelope getEnvelope() {
        Envelope e = new Envelope();
        this.forEach( (v) -> e.expandToInclude( v.getGeometry().getEnvelopeInternal() ) );
        return e;
    }

    /**
     * Get convex hull of the map
     * @return
     */
    public ConvexHull convexHull() {
        Coordinate[] points_list = this.stream().flatMap(
                (il)-> Arrays.stream(il.getGeometry().getCoordinates())).toArray((size)->new Coordinate[size]);
        return new ConvexHull(points_list,gf);
    }

    /**
     * Retrive specific isolines by predicate
     * @param predicate if predicate returned true, isoline will be included in return list
     * @return IsolineContariner with isolines matching predicate
     */
    public IsolineContainer filter( Predicate<? super IIsoline> predicate ) {
        return new IsolineContainer( this.gf,this.stream().filter( predicate ) );
    }

    /**
     * Perform a connection operation to all lines.
     * isoline collection will be transformed, some isolines will be removed, new added.
     * It uses greedy algorithm which gives priority to stronger and valid connections
     * over weak ones.
     */
    /*
    public void connectLines(double min_angle,double max_angle,double max_dist, double weld_dist) {
        ConnectionEvaluator eval = new ConnectionEvaluator(min_angle,max_angle,max_dist,weld_dist);

        // Get line ends
        Map<IIsoline,Pair<LineEnd,LineEnd>> line_ends = getLineEnds(this.stream());

        // Get connections
        List< Pair<Connection,Double> > cons = getConnections(
                line_ends.values().stream().flatMap(
                        (x) -> Stream.of(x.getKey(), x.getValue())
                ).collect(Collectors.toList()),
                eval);

        // Sort by descending order, so we give priority to heavier connections
        Collections.sort(cons,(lhs,rhs)->-lhs.getValue().compareTo(rhs.getValue()));

        ListIterator< Pair<Connection,Double> > it = cons.listIterator(0);

        for (Pair<Connection,Double> p: cons) {
            Connection con = p.getKey(); // no need for score value, they are already sorted

            // Oops, looks like this connection refers to already merged line end
            if (!con.isValid()) continue;

            IIsoline l = connect(con,gf);
            if (l == null) continue;
            if (con.first().isoline != con.second().isoline) { // Connected DIFFERENT lines
                // Now we must update line ends to fit model after merging
                // Get ends of merged lines
                Pair<LineEnd,LineEnd> first_le_p = line_ends.get(con.first().isoline);
                Pair<LineEnd,LineEnd> second_le_p = line_ends.get(con.second().isoline);

                if (first_le_p == null || second_le_p == null) {
                    throw new RuntimeException("At this point, there MUST be lineEnds for isoline");
                }

                // Remove line ends from line ends pool
                line_ends.remove(con.first().isoline);
                line_ends.remove(con.second().isoline);

                // Remove merged lines from isoline pool
                remove(con.first().isoline);
                remove(con.second().isoline);

                // Remove reference to merged isolines from thier ends
                // Do so, because futher we'll update these references to newly formed values
                // And merged LineEnds will contain reference to null
                first_le_p.getKey().isoline = null;
                first_le_p.getValue().isoline = null;
                second_le_p.getKey().isoline = null;
                second_le_p.getValue().isoline = null;

                // Estimate remaining ends
                LineEnd first_le = con.first().end_index == -1 ? first_le_p.getKey() : first_le_p.getValue();
                LineEnd second_le = con.second().end_index == -1 ? second_le_p.getKey() : second_le_p.getValue();

                // Because points of first line are guaranteed to be first,
                // we can assign it's end index to 1.
                first_le.end_index = 1;
                first_le.isoline = l;
                second_le.end_index = -1;
                second_le.isoline = l;


                // Add line ends to line end pool
                // no need to update connection pool, though,
                // because it refers to isolines through LineEnd.isoline, which we just updated
                // You'll just need to check for LineEnd's isoline being null futher in loop
                // this means that current connection refers to merged line end.
                line_ends.put(l,new Pair<>(first_le,second_le));
            } else { // Created line loop
                // No need for updating line ends, just remove them.
                line_ends.remove(con.first().isoline);
                remove(con.first().isoline);

                con.first().isoline = null;
                con.second().isoline = null;
            }
            // Add newly formed line
            add(l);

        }
    }
*/
    /**
     * Determine, weather the line end is on the outerBound of the map
     * (ray, traced by this line end does not intersect any other lines)
     * @param le
     * @return True, if line end is on map outerBound
     */
    public boolean isEdgeLineEnd(LineEnd le, ConvexHull convexHull) {
        LineString ch_shell = ((Polygon)convexHull.getConvexHull()).getExteriorRing();
        Coordinate c1 = le.line.p1;
        Coordinate c2 = GeomUtils.closestPoint(c1,ch_shell);
        final LineString hull_ls = gf.createLineString( new Coordinate[] {c1,c2} );
        if (!this.stream().anyMatch((x)-> x.getLineString().crosses(hull_ls))) {
            return true;
        }
        return false;
    }

//    /**
//     * Retrieve collection of possible connections between line ends and their scores (so called distance)
//     * @param lineEnds collection of line ends
//     * @param eval function, accepting connection and returning it's score (distance)
//     * @return map Connection as value with it's score as value
//     */
    /*
    public List< Pair<Connection,Double> > getConnections(List<LineEnd> lineEnds,
                                                          Function<Connection, Double> eval) {

        class ConnectionAttributes {

            public ConnectionAttributes(Connection connection, double score, LineString ls) {
                this.connection = connection;
                this.score = score;
                this.connecting_line = ls;
            }

            public Connection connection;
            public double score;
            public LineString connecting_line;
        }

        List<ConnectionAttributes> ret = new LinkedList<>();
        Connection con = new Connection();
        for (int i = 0; i != lineEnds.size(); ++i) {
            LineEnd l1 = lineEnds.get(i);
            for (int j = i+1; j < lineEnds.size(); ++j) {
                LineEnd l2 = lineEnds.get(j);
                if (con.SetLineEnds(l1,l2)) {
                    if (con.isValid()) {
                        Double score = eval.apply(con);

                        double score2 = parallelScore(con)/2;
                        score+=score2;

                        final LineString con_line = con.getConnectionLine(gf);

                        if (score > 0) {
                            if (this.stream().anyMatch((iline) -> iline.getLineString().crosses(con_line))) {
                                score = -1.0;
                            }
                        }

                        if (score > 0) {

                            LineString con_ls = con.getConnectionLine(gf);

                            for (ConnectionAttributes con_attr : ret) {
                                if ( con_attr.connecting_line.intersects(con_ls) &&
                                        con_attr.connecting_line.getStartPoint().distance(con_ls) > 0.002 &&
                                        con_attr.connecting_line.getEndPoint().distance(con_ls) > 0.002) {
                                    con_attr.score *= 0.5;
                                    score *= 0.5;
                                }
                            }

                            ret.add(new ConnectionAttributes(con, score, con_line));
                            con = new Connection();
                        }

                    }
                }
            }
        }

        return ret.stream().map((cattr)-> new Pair<>(cattr.connection, cattr.score)).collect(Collectors.toList());
    }*/


    public static List<Coordinate> getEndPointArray(LineString ls, int end_index, double offset, int iterations) {
        offset = offset/ls.getLength();
        double step_size = offset/iterations;

        if (offset > 0.3)
            offset = 0.3;

        if (end_index == 1)
            return Interpolator.InterpolateAlongLocal(ls,0,offset,step_size);
        else
            return Interpolator.InterpolateAlongLocal(ls,1,1-offset,step_size);
    };

    public GeometryFactory getFactory() {
        return gf;
    }


    /*
    public static HashMap<IIsoline,Pair<LineEnd,LineEnd>> getLineEnds(Stream<IIsoline> ils ) {
        HashMap<IIsoline,Pair<LineEnd,LineEnd>> ret = new HashMap<>();
        ils.forEach((iline)->{
                    LineEnd l1 = LineEnd.fromIsoline(iline,1);
                    LineEnd l2 = LineEnd.fromIsoline(iline,-1);
                    if (l1 != null && l2 != null) {
                        ret.put(iline,new Pair<>(l1,l2));
                    }
                }
        );
        return ret;
    }*/


}
