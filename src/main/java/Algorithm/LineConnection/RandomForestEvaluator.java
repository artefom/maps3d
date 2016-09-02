package Algorithm.LineConnection;

import Utils.*;
import Utils.Area.CoordinateAttributed;
import Utils.Area.LSWAttributed;
import Utils.Area.LineSegmentWrapper;
import Utils.Area.PointAreaBuffer;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.*;

import java.io.InputStream;
import java.util.*;

/**
 * Created by Artyom.Fomenko on 18.08.2016.
 */
public class RandomForestEvaluator {


    /**
     * Name and ids of features
     */


    private static final int signed_angle_sum = 0;
    private static final int angle_sum = 1;
    private static final int angle_sum_equals = 2;
    private static final int signed_angle_more_than_PI = 3;
    private static final int distance = 4;
    private static final int distance_closest_line = 5;
    private static final int score = 6;
    private static final int cons_score_max = 7;
    private static final int cons_score_mean = 8;
    private static final int cons_curMax = 9;
    private static final int inters_score_max = 10;
    private static final int inters_score_mean = 11;
    private static final int strong = 12;
    private static final int groupID = 13;
    public static final String[] feature_names = new String[] {
        "signed_angle_sum","angle_sum","angle_sum_equals","signed_angle_more_than_PI","distance","distance_closest_line"
            ,"score","cons_score_max","cons_score_mean","cons_curMax","inters_score_max","inters_score_mean","strong","GroupID"};


    public static class Connection_attributed {
        public Connection con;

        /* Feature ids:

            0: angle1
            1: angle2
            2: distance
            3: score
            4: cons_score_max
            5: cons_score_mean
            6: cons_curMax
            7: inters_score_max
            8: inters_score_mean
        */

        public double[] features = new double[feature_names.length];

        public Connection_attributed(Connection con) {
            this.con = con;

            features[cons_score_max] =  -1000;
            features[cons_score_mean] = 0;
            features[cons_curMax] = 0;
            features[inters_score_max] = -1000;
            features[inters_score_mean] = 0;
        }

        public Connection core() {
            return con;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Connection_attributed that = (Connection_attributed) o;

            return con != null ? con.equals(that.con) : that.con == null;
        }

        @Override
        public int hashCode() {
            return con.hashCode();
        }
    }

    public CachedTracer<Isoline_attributed> intersector;
    GeometryFactory gf = null;


    /**
     * Values are madeup with linear regression
     * @param con
     * @return
     */
    public static double getDraftScore(Connection_attributed con) {
        //return 0.21 + con.features[angle_sum]*0.88 + con.features[angle2]*0.88 - con.features[distance]*0.00333;
        return 0;
    }

    public static final double draftScoreThreshold = 0.138;
    public static final double finalScoreEqualPrecisionRecallThreshold = 0.47;
    public static final double finalSocre95PercentPrecisionThreshold = 0.8;
    public static final double finalScore95PercentRecallThreshold = 0.2;

    private Pair<Double,Double> getLineToPointScores(Coordinate l1, Coordinate l2, Coordinate c) {
        double ang = Angle.angleBetweenOriented(l1,l2,c);
        //ang = (((Math.PI*90/180)*0.5)-ang)/( (Math.PI*90/180) *0.5);
        double dist = l2.distance(c);
        return new Pair<>(ang,dist);
    }

    public void applyAngleDistanceScores( Connection_attributed con ) {
        LineSegment line1 = con.core().first().line;
        LineSegment line2 = con.core().second().line;

        double dist = line1.p1.distance(line2.p1);
        double ang1 = Angle.angleBetweenOriented(line1.p0,line1.p1,line2.p1);
        double ang2= Angle.angleBetweenOriented(line2.p0,line2.p1,line1.p1);

        con.features[distance] = dist;
        con.features[angle_sum] = (Math.PI*2)-( Math.abs(ang1)+Math.abs(ang2) );
        con.features[signed_angle_sum] = Math.abs(ang1+ang2);
        con.features[angle_sum_equals] = ((ang1 < 0 && ang2 < 0) || (ang1 > 0 && ang2 >0)) ? 1 : 0;
        con.features[signed_angle_more_than_PI] = (con.features[signed_angle_sum] > Math.PI) ? 1 : 0;
    }



//    e: -1.21071517609
//    angle_sum: -1.56471653345
//    distance: -0.0247778080897

    private static final double group0_intercept = -1.21071517609;
    private static final double group0_b_ang_sum = -1.56471653345;
    private static final double group0_b_distanc = -0.0247778080897;


//    e: 2.66559878786
//    angle_sum: -1.50952504187
//    signed_angle_sum: -0.887980817565
//    distance: -0.0664416824728

    private static final double group1_intercept = 2.66559878786;
    private static final double group1_angle_sum_b = -1.50952504187;
    private static final double group1_signed_angle_sum_b = -0.887980817565;
    private static final double group1_distance_b = -0.0664416824728;

//    e: 2.29197346712
//    angle_sum: -1.51532817942
//    distance: -0.0572548732509
//
    private static final double group2_intercept = 2.29197346712;
    private static final double group2_angle_sum_b = -1.51532817942;
    private static final double group2_distance_b = -0.0572548732509;

    private static double group0DraftScore(Connection_attributed con) {
        return 1.0/(1+Math.exp(-(group0_intercept+con.features[angle_sum]*group0_b_ang_sum+con.features[distance]*group0_b_distanc)));
    }

    private static double group1DraftScore(Connection_attributed con) {
        return 1.0/(1+Math.exp(-(group1_intercept +con.features[signed_angle_sum]*group1_signed_angle_sum_b+
                con.features[angle_sum]*group1_angle_sum_b+con.features[distance]*group1_distance_b)));
    }

    private static double group2DraftScore(Connection_attributed con) {
        return 1.0/(1+Math.exp(-(group2_intercept+con.features[angle_sum]*group2_angle_sum_b+con.features[distance]*group2_distance_b)));
    }

    private static int getGrpoupID( Connection_attributed con ) {

        if (con.features[angle_sum_equals] == 1 && con.features[signed_angle_more_than_PI] == 1) {
            return 0;
        }

        if ( (con.features[angle_sum_equals] == 0) && (con.features[signed_angle_more_than_PI] == 0)) {
            return 1;
        }
        return 2;
    }

    private static double generateDraftScore( Connection_attributed con ) {

        if ( Double.isNaN( con.features[angle_sum] ) ) return 0;
        if ( Double.isNaN( con.features[angle_sum] ) ) return 0;
        if ( con.features[angle_sum] > (Math.PI*2)-0.01 ) return 0;
        if ( con.features[signed_angle_sum] > (Math.PI*2)-0.01 ) return 0;
        if ( Math.abs(con.features[signed_angle_sum]-Math.PI) < 0.01 ) return 0;
        if (  (con.features[angle_sum_equals] == 1) & (con.features[signed_angle_more_than_PI] == 0) ) return 0;

        if ( (con.features[angle_sum] ) < 0.001  ) return 1;

        int group_id = getGrpoupID(con);
        if ( group_id == 0 ) {
            return group0DraftScore(con);
        }

        if (  group_id == 1 ) {
            return group1DraftScore(con);
        }

        return group2DraftScore(con);
    }

    Set<LineSegmentWrapper> ret_buf = Collections.newSetFromMap(new IdentityHashMap<LineSegmentWrapper,Boolean>());
    private void addIfNotIntersectsAdvanced(ArrayList<Connection_attributed> cons, LineEnd le1, LineEnd le2, boolean reduced) {
        if (le1 == null || le2 == null) return;

        LineSegment seg = new LineSegment(le1.line.p1,le2.line.p1);


        Connection_attributed con = new Connection_attributed( Connection.fromLineEnds(le1,le2) );

        if (con.con == null || reduced && !con.con.isValid()) return;
        applyAngleDistanceScores(con);

        con.features[strong] = con.con.first().isoline.getIsoline().isHalf() ? 0 : 1;

        double draft_score = generateDraftScore(con);

        if (draft_score == 1 || draft_score == 0) return;

        con.features[groupID] = getGrpoupID(con);
        con.features[score] = draft_score;

//        if ( con.features[angle_sum] > (Math.PI*2)-0.01 ) return;
//        if ( con.features[signed_angle_sum] > (Math.PI*2)-0.01 ) return;
//        if ( Math.abs(con.features[signed_angle_sum]-Math.PI) < 0.01 ) return;

        // Values, very close to 0
        //if ( con.features[angle_sum] <  0.01 || con.features[signed_angle_sum] < 0.01  ) return;

//        con.features[angle_sum] < Math.PI-( Math.abs(ang1)+Math.abs(ang2) );
//        con.features[signed_angle_sum] = Math.abs(ang1+ang2);

        double length = con.con.connectionSegment.getLength();
        if (length > Constants.CONNECTIONS_MAX_DIST || length < Constants.CONNECTIONS_WELD_DIST) return;

//        if (reduced) {
//            con.features[score] = getDraftScore(con);
//            if (con.features[score] < draftScoreThreshold)  return;
//        }
//
//        if (reduced && (con.features[score] < draftScoreThreshold))  return;


        if (intersector.intersects(seg,0.01,0.99)) return;

        ret_buf.clear();

        intersector.buffer.findInArea(con.con.connectionSegment.p0,Constants.CONNECTIONS_WELD_DIST,ret_buf);
        intersector.buffer.findInArea(con.con.connectionSegment.p1,Constants.CONNECTIONS_WELD_DIST,ret_buf);
        double dist_to_line = Constants.CONNECTIONS_WELD_DIST;

        LineSegment con_seg = con.con.getConnectionSegment();
        Coordinate con_p0 = con_seg.p0;
        Coordinate con_p1 = con_seg.p1;

        for (LineSegmentWrapper lsw_d : ret_buf) {
            LSWAttributed<Isoline_attributed> lsw = (LSWAttributed<Isoline_attributed>)lsw_d;

            if ((lsw.getBeginX() != con_p0.x && lsw.getBeginY() != con_p0.y && lsw.getEndX() != con_p1.x && lsw.getEndY() != con_p1.y)&&
                    (lsw.getBeginX() != con_p1.x && lsw.getBeginY() != con_p1.y && lsw.getEndX() != con_p0.x && lsw.getEndY() != con_p0.y) ) {
                dist_to_line = Math.min( dist_to_line, con_seg.distance(lsw.getSegment()) );
            }
        }

        if (dist_to_line < Constants.CONNECTIONS_WELD_DIST*0.1) return;

        //con.features[distance_closest_line] = dist_to_line;

        cons.add(con);
    }


    private static void gatherSourceData(Connection_attributed con, ArrayList<Connection_attributed> cons) {

        ArrayList<Double> scores = new ArrayList<>();
        ArrayList<Double> intersecting_scores = new ArrayList<>();
        LineEnd first = con.core().first();
        LineEnd second = con.core().second();
        for (Connection_attributed con2 : cons) {

            LineEnd first2 = con2.core().first();
            LineEnd second2 = con2.core().second();

            if ((first2   == first && second2 == second) ||
                    (second2  == first && first2  == second))
                continue;
            if (first2    == first || first2     == second ||
                    second2   == first || second2    == second) {
                scores.add( getDraftScore(con2) );

            }

            if (CachedTracer.intersects(con.core().getConnectionSegment(),con2.core().getConnectionSegment(),0.01,0.99)) {
                intersecting_scores.add(getDraftScore(con2));
            }
        }

//        4: cons_score_max
//        5: cons_score_mean
//        6: cons_curMax
//        7: inters_score_max
//        8: inters_score_mean

        if (scores.size() > 0) {
            con.features[cons_score_max] = scores.stream().max(Double::compare).get();
            con.features[cons_score_mean] = scores.stream().reduce((lhs, rhs) -> lhs + rhs).get() / scores.size();
            con.features[cons_curMax] = con.features[cons_score_max] < getDraftScore(con) ? 1 : 0;
        }

        if (intersecting_scores.size() > 0) {
            con.features[inters_score_max] = intersecting_scores.stream().max(Double::compare).get();
            con.features[inters_score_mean] = intersecting_scores.stream().reduce((lhs, rhs) -> lhs + rhs).get() / intersecting_scores.size();
        }

    }

    /**
     * Return list of connections with features
     * @return
     */
    public ArrayList< Connection_attributed > getConnections(ArrayList<Isoline_attributed> isos_attributed, GeometryFactory gf, boolean reduced) {
        /*Asess by features:
           'angle1', 'angle2', 'distance', 'score', 'cons_score_max', 'cons_score_mean',
           'cons_curMax', 'inters_score_max',
           'inters_score_mean'
         */

        this.gf = gf;

        ArrayList<Connection_attributed> pre_ret = new ArrayList<>();
        ArrayList<CoordinateAttributed<LineEnd>> lineEndPool = new ArrayList<>();

        for (Isoline_attributed iso : isos_attributed) {
            LineEnd le1 = LineEnd.fromIsoline(iso,-1);
            LineEnd le2 = LineEnd.fromIsoline(iso,1);

            if (le1 != null) lineEndPool.add(new CoordinateAttributed<>( le1.line.p1, le1 ));
            if (le2 != null) lineEndPool.add(new CoordinateAttributed<>( le2.line.p1, le2 ));
        }

        PointAreaBuffer<LineEnd> lineEndAreaBuffer = new PointAreaBuffer<>();
        lineEndAreaBuffer.setEnvelope(lineEndPool);
        lineEndAreaBuffer.init(100,100);
        lineEndAreaBuffer.addAll(lineEndPool);

        Set<CoordinateAttributed<LineEnd>> candidates = Collections.newSetFromMap(new IdentityHashMap<CoordinateAttributed<LineEnd>,Boolean>());

        CommandLineUtils.reportProgressBegin("Collecting connections");
        int current = 0;
        for (CoordinateAttributed<LineEnd> coordinate_line_end1 : lineEndPool) {
            CommandLineUtils.reportProgress(++current,lineEndPool.size());
            LineEnd le1 = coordinate_line_end1.entity;
            candidates.clear();
            lineEndAreaBuffer.findInArea(coordinate_line_end1.x,coordinate_line_end1.y, Constants.CONNECTIONS_MAX_DIST,candidates);

            for (CoordinateAttributed<LineEnd> coordinate_line_end2 : candidates)
            {
                LineEnd le2 = coordinate_line_end2.entity;
                if (le1 != le2) {
                    addIfNotIntersectsAdvanced(pre_ret, le1,le2,reduced);
                }
            }

            lineEndAreaBuffer.remove(coordinate_line_end1);

        }
        CommandLineUtils.reportProgressEnd();

        System.out.println("Gathered "+pre_ret.size()+" possible connections");

        CommandLineUtils.reportProgressBegin("Extracting connection features");
        current = 0;
        for (Connection_attributed con: pre_ret) {
            CommandLineUtils.reportProgress(++current,pre_ret.size());
            gatherSourceData(con,pre_ret);
        }
        CommandLineUtils.reportProgressEnd();

        return pre_ret;
    }

    public RandomForestEvaluator(CachedTracer<Isoline_attributed> intersector) {
        this.intersector = intersector;
    }

    public static ArrayList< Connection > evaluateConnectionsRandomForest(ArrayList< Connection_attributed > pre_ret, String path ) {

        RandomForestRegressor regressor = new RandomForestRegressor();
        try {
            ClassLoader classLoader = RandomForestEvaluator.class.getClassLoader();
            InputStream is = classLoader.getResourceAsStream("forest.txt");
            regressor.loadModelFromFile(is);
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
            return null;
        }

        ArrayList<Connection> result = new ArrayList<>();

        CommandLineUtils.reportProgressBegin("Evalutating connections");
        int current = 0;
        for (Connection_attributed c_atr : pre_ret) {
            CommandLineUtils.reportProgress(++current,pre_ret.size());
            Connection core = c_atr.core();
            core.score = regressor.predict(c_atr.features);
            result.add(core);
        }
        CommandLineUtils.reportProgressEnd();

        return result;
    }

//    public ArrayList<Pair<Connection,double[]>> getFeatures(ArrayList<Isoline_attributed> isos_attributed, GeometryFactory gf) {
//                /*Asess by features:
//           'angle1', 'angle2', 'distance', 'score', 'cons_score_max', 'cons_score_mean',
//           'cons_curMax', 'inters_score_max',
//           'inters_score_mean'
//         */
//
//        this.gf = gf;
//
//
//        ArrayList<Connection_attributed> pre_ret = new ArrayList<>();
//        /*Run through all pairs of isolines*/
//
//        CommandLineUtils.reportProgressBegin("Gathering connections");
//        for (int i = 0; i != isos_attributed.size(); ++i) {
//            CommandLineUtils.reportProgress(i,isos_attributed.size());
//            CommandLineUtils.reportProgress(i,isos_attributed.size());
//            Isoline_attributed i1 = isos_attributed.get(i);
//
//            // Add connection line to itself
//            addIfNotIntersectsAdvanced(pre_ret, i1.begin,i1.end, false );
//
//            for (int j = i+1; j < isos_attributed.size(); ++j) {
//                Isoline_attributed i2 = isos_attributed.get(j);
//
//                if (i2.getType() == i1.getType()) {
//                    addIfNotIntersectsAdvanced(pre_ret, i1.begin, i2.begin);
//                    addIfNotIntersectsAdvanced(pre_ret, i1.begin, i2.end);
//                    addIfNotIntersectsAdvanced(pre_ret, i1.end, i2.begin);
//                    addIfNotIntersectsAdvanced(pre_ret, i1.end, i2.end);
//                }
//            }
//        }
//        CommandLineUtils.reportProgressEnd();
//
//        for (Connection_attributed con: pre_ret) {
//            gatherSourceData(con,pre_ret);
//        }
//
//        ArrayList<Pair<Connection,double[]>> ret = new ArrayList<>();
//
//        for (Connection_attributed c_atr : pre_ret) {
//            ret.add(new Pair<>( c_atr.core(), c_atr.features ) );
//        }
//
//        return ret;
//    }
}
