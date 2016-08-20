package Algorithm.LineConnection;

import Algorithm.DatasetGenerator.Datagen;
import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.*;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by Artyom.Fomenko on 18.08.2016.
 */
public class RandomForestEvaluator {


    /**
     * Name and ids of features
     */
    private static final int angle1 = 0;
    private static final int angle2 = 1;
    private static final int distance = 2;
    private static final int score = 3;
    private static final int cons_score_max = 4;
    private static final int cons_score_mean = 5;
    private static final int cons_curMax = 6;
    private static final int inters_score_max = 7;
    private static final int inters_score_mean = 8;
    public static final String[] feature_names = new String[] {"angle1","angle2","distance","score","cons_score_max","cons_score_mean","cons_curMax","inters_score_max","inters_score_mean"};


    private class Connection_attributed {
        private Connection con;

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

        private double[] features = new double[9];

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
    }

    public Intersector intersector;
    GeometryFactory gf = null;


    /**
     * Values are madeup with linear regression
     * @param con
     * @return
     */
    public static double getDraftScore(Connection_attributed con) {
        return 0.21 + con.features[angle1]*0.88 + con.features[angle2]*0.88 - con.features[distance]*0.00333;
    }

    public static final double draftScoreThreshold = 0.138;
    public static final double finalScoreEqualPrecisionRecallThreshold = 0.47;
    public static final double finalSocre95PercentPrecisionThreshold = 0.8;
    public static final double finalScore95PercentRecallThreshold = 0.2;

    private Pair<Double,Double> getLineToPointScores(Coordinate l1, Coordinate l2, Coordinate c) {
        double ang = Math.PI- Angle.angleBetween(l1,l2,c);
        ang = (((Math.PI*90/180)*0.5)-ang)/( (Math.PI*90/180) *0.5);
        double dist = l2.distance(c);
        return new Pair<>(ang,dist);
    }

    public void applyAngleDistanceScores( Connection_attributed con ) {
        LineSegment line1 = con.core().first().line;
        LineSegment line2 = con.core().second().line;
        Pair<Double,Double> scores1 = getLineToPointScores(line1.p0,line1.p1,line2.p1);
        Pair<Double,Double> scores2 = getLineToPointScores(line2.p0,line2.p1,line1.p1);
        con.features[distance] = (scores1.v2+scores2.v2)*0.5;
        con.features[angle1] = scores1.v1;
        con.features[angle2] = scores2.v1;
    }

    private void addIfNotIntersectsAdvanced(ArrayList<Connection_attributed> cons, LineEnd le1, LineEnd le2) {
        if (le1 == null || le2 == null) return;

        LineSegment seg = new LineSegment(le1.line.p1,le2.line.p1);
        if (intersector.intersects(seg)) return;

        Connection_attributed con = new Connection_attributed( Connection.fromLineEnds(le1,le2) );

        applyAngleDistanceScores(con);

        con.features[score] = getDraftScore(con);

        if (con.features[score] < draftScoreThreshold) return;

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

            if (Tracer.intersects(con.core().getConnectionSegment(),con2.core().getConnectionSegment(),0.01,0.99)) {
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
     * Return list of evaluated connections
     * @return
     */
    public ArrayList< Connection > getConnections(ArrayList<Isoline_attributed> isos_attributed, GeometryFactory gf) {
        /*Asess by features:
           'angle1', 'angle2', 'distance', 'score', 'cons_score_max', 'cons_score_mean',
           'cons_curMax', 'inters_score_max',
           'inters_score_mean'
         */

        this.gf = gf;

        Intersector intersector = new Intersector(isos_attributed.stream().map((x) -> x.getIsoline().getGeometry()).collect(Collectors.toList()), gf);

//        ArrayList<Algorithm.LineConnection.Isoline_attributed> isos_attributed = new ArrayList<>(isolines.size());
//        for (Isoline_attributed cmap : isolines) {
//            Algorithm.LineConnection.Isoline_attributed i_attr = new Algorithm.LineConnection.Isoline_attributed(cmap);
//            isos_attributed.add(i_attr);
//        }


        ArrayList<Connection_attributed> pre_ret = new ArrayList<>();
        /*Run through all pairs of isolines*/
        for (int i = 0; i != isos_attributed.size(); ++i) {
            CommandLineUtils.reportProgress(i,isos_attributed.size());
            Isoline_attributed i1 = isos_attributed.get(i);

            // Add connection line to itself
            addIfNotIntersectsAdvanced(pre_ret, i1.begin,i1.end );

            for (int j = i+1; j < isos_attributed.size(); ++j) {
                Isoline_attributed i2 = isos_attributed.get(j);

                if (i2.getType() == i1.getType()) {
                    addIfNotIntersectsAdvanced(pre_ret, i1.begin, i2.begin);
                    addIfNotIntersectsAdvanced(pre_ret, i1.begin, i2.end);
                    addIfNotIntersectsAdvanced(pre_ret, i1.end, i2.begin);
                    addIfNotIntersectsAdvanced(pre_ret, i1.end, i2.end);
                }
            }
        }

        ArrayList<Connection> scores = new ArrayList<>();

        for (Connection_attributed con: pre_ret) {
            gatherSourceData(con,pre_ret);
        }

        RandomForestRegressor regressor = new RandomForestRegressor();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream is = classLoader.getResourceAsStream("forest.txt");
            regressor.loadModelFromFile(is);
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
            return null;
        }

        for (Connection_attributed c_atr : pre_ret) {
            Connection core = c_atr.core();
            core.score = regressor.predict(c_atr.features);
            scores.add(core);
        }

        return scores;
    }

    public ArrayList<Pair<Connection,double[]>> getFeatures(ArrayList<Isoline_attributed> isos_attributed, GeometryFactory gf) {
                /*Asess by features:
           'angle1', 'angle2', 'distance', 'score', 'cons_score_max', 'cons_score_mean',
           'cons_curMax', 'inters_score_max',
           'inters_score_mean'
         */

        this.gf = gf;

        Intersector intersector = new Intersector(isos_attributed.stream().map((x) -> x.getIsoline().getGeometry()).collect(Collectors.toList()), gf);

//        ArrayList<Algorithm.LineConnection.Isoline_attributed> isos_attributed = new ArrayList<>(isolines.size());
//        for (Isoline_attributed cmap : isolines) {
//            Algorithm.LineConnection.Isoline_attributed i_attr = new Algorithm.LineConnection.Isoline_attributed(cmap);
//            isos_attributed.add(i_attr);
//        }


        ArrayList<Connection_attributed> pre_ret = new ArrayList<>();
        /*Run through all pairs of isolines*/

        CommandLineUtils.reportProgressBegin("Gathering connections");
        for (int i = 0; i != isos_attributed.size(); ++i) {
            CommandLineUtils.reportProgress(i,isos_attributed.size());
            CommandLineUtils.reportProgress(i,isos_attributed.size());
            Isoline_attributed i1 = isos_attributed.get(i);

            // Add connection line to itself
            addIfNotIntersectsAdvanced(pre_ret, i1.begin,i1.end );

            for (int j = i+1; j < isos_attributed.size(); ++j) {
                Isoline_attributed i2 = isos_attributed.get(j);

                if (i2.getType() == i1.getType()) {
                    addIfNotIntersectsAdvanced(pre_ret, i1.begin, i2.begin);
                    addIfNotIntersectsAdvanced(pre_ret, i1.begin, i2.end);
                    addIfNotIntersectsAdvanced(pre_ret, i1.end, i2.begin);
                    addIfNotIntersectsAdvanced(pre_ret, i1.end, i2.end);
                }
            }
        }
        CommandLineUtils.reportProgressEnd();

        for (Connection_attributed con: pre_ret) {
            gatherSourceData(con,pre_ret);
        }

        ArrayList<Pair<Connection,double[]>> ret = new ArrayList<>();

        for (Connection_attributed c_atr : pre_ret) {
            ret.add(new Pair<>( c_atr.core(), c_atr.features ) );
        }

        return ret;
    }
}
