package Algorithm.DatasetGenerator;

import Algorithm.LineConnection.*;
import Algorithm.NearbyGraph.Isoline_attributed;
import Algorithm.NearbyGraph.NearbyContainer;
import Algorithm.NearbyGraph.NearbyEstimator;
import Algorithm.NearbyGraph.NearbyGraphWrapper;
import Isolines.IIsoline;
import Isolines.Isoline;
import Isolines.IsolineContainer;
import Utils.*;
import Utils.Area.LSWAttributed;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import com.vividsolutions.jts.operation.valid.SimpleNestedRingTester;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Artyom.Fomenko on 16.08.2016.
 */
public class Datagen {


    IsolineContainer cont;
    public Datagen(IsolineContainer cont) {
        this.cont = cont;
        cont.serialize("sample_isolines.json");
    }

    public static void cutMap(IsolineContainer cont) {
        ArrayList<Polygon> circles = new ArrayList<>();
    }


    public static ArrayList<IIsoline> cutCircle(ArrayList<IIsoline> isolines, ArrayList<LineString> cutted_lines, Geometry cirlce) {
        ArrayList<IIsoline> result = new ArrayList<>();

        Envelope c_env = cirlce.getEnvelopeInternal();

        for (int i = isolines.size()-1; i >= 0; --i) {
            IIsoline iso = isolines.get(i);

            if (!iso.getLineString().getEnvelopeInternal().intersects(c_env)) {
                result.add(new Isoline(iso));
                continue;
            }

            Geometry dif = iso.getLineString().difference(cirlce);
            Geometry intersection = iso.getLineString().intersection(cirlce);
            for (int geom_num = 0; geom_num != dif.getNumGeometries(); ++geom_num) {
                LineString ls = (LineString)dif.getGeometryN(geom_num);

                Isoline new_iso = new Isoline( iso.getType(),iso.getSlopeSide(),ls.getCoordinateSequence(),iso.getFactory() );
                new_iso.setHeight(iso.getHeight());
                new_iso.setEdgeToEdge(false);;
                new_iso.setID( iso.getID() );
                if (new_iso.getLineString().getNumPoints() >= 2)
                    result.add(new_iso);
            }

            for (int geom_num = 0; geom_num != intersection.getNumGeometries(); ++geom_num) {
                Geometry ls = intersection.getGeometryN(geom_num);
                if (ls instanceof LineString)
                    if (ls.getNumPoints() > 1) cutted_lines.add((LineString)ls);
            }
        }

        return result;
    }

    private  static Random rand = new Random();
    private static double genRandom(double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }

    public static Pair<ArrayList<LineString>,IsolineContainer> genData(IsolineContainer cont) {
        GeometricShapeFactory gsf = new GeometricShapeFactory();
        GeometryFactory gf = cont.getFactory();
        Envelope env = cont.getEnvelope();

        ArrayList<IIsoline> isolines = new ArrayList<>();
        for ( IIsoline iso : cont ) {
            Isoline new_isoline = new Isoline(iso);
            isolines.add(new_isoline);
        }

        ArrayList<LineString> cuttedLines = new ArrayList<>();
        int cut_count = 250;
        CommandLineUtils.reportProgressBegin("Cutting "+cut_count+" squares");
        for (int i = 0; i != cut_count; ++i) {
            CommandLineUtils.reportProgress(i,cut_count);
            double x = genRandom(env.getMinX(),env.getMaxX());
            double y = genRandom(env.getMinY(),env.getMaxY());
            double aspect = genRandom(0.1,10);
            double area = genRandom(1000,3000);
            double height = Math.sqrt(area/aspect);
            double width = height*aspect;
            double rotation = genRandom(-Math.PI,Math.PI);
            gsf.setCentre(new Coordinate(x,y));
            gsf.setWidth(width);
            gsf.setHeight(height);
            gsf.setRotation(rotation);
            Polygon rect = gsf.createRectangle();
            isolines = cutCircle(isolines,cuttedLines,rect);
        }
        CommandLineUtils.reportProgressEnd();

        return new Pair<>(cuttedLines, new IsolineContainer(gf,isolines));
    }

    public static void genData(String outPath) {
        System.out.println("Generating data...");

        IsolineContainer cont = null;
        try {
            cont = IsolineContainer.deserialize("cached_isolines.json");
        } catch (Exception ex) {
            CommandLineUtils.printError("Could not load data from cached_isolines.json");
            throw new RuntimeException(ex.getMessage());
        }
        System.out.println( "Loaded " + cont.size() + " isolines" );

        ArrayList<IIsoline> isolines = new ArrayList<>();
        for ( IIsoline iso : cont ) {
            Isoline new_isoline = new Isoline(iso);
            isolines.add(new_isoline);
        }

        GeometricShapeFactory gsf = new GeometricShapeFactory();
        GeometryFactory gf = new GeometryFactory();
        Envelope env = cont.getEnvelope();

        ArrayList<LineString> cuttedLines = new ArrayList<>();


        CommandLineUtils.reportProgressBegin("Cutting squares");
        for (int i = 0; i != 500; ++i) {
            CommandLineUtils.reportProgress(i,500);
            //System.out.println(i);
            double x = genRandom(env.getMinX(),env.getMaxX());
            double y = genRandom(env.getMinY(),env.getMaxY());
            double aspect = genRandom(0.1,10);
            double area = genRandom(10,30);
            double height = Math.sqrt(area/aspect);
            double width = height*aspect;
            double rotation = genRandom(-Math.PI,Math.PI);
            gsf.setCentre(new Coordinate(x,y));
            gsf.setWidth(width);
            gsf.setHeight(height);
            gsf.setRotation(rotation);
            Polygon rect = gsf.createRectangle();
            isolines = cutCircle(isolines,cuttedLines,rect);
        }
        CommandLineUtils.reportProgressEnd();

        CommandLineUtils.reportProgressBegin("Cutting lines");
        for (int i = 0; i != 100; ++i) {
            CommandLineUtils.reportProgress(i,100);
            LineString ls = gf.createLineString(new Coordinate[]{
                    new Coordinate( genRandom(env.getMinX(),env.getMaxX()), genRandom(env.getMinY(),env.getMaxY())),
                    new Coordinate( genRandom(env.getMinX(),env.getMaxX()), genRandom(env.getMinY(),env.getMaxY()))
            });

            isolines = cutCircle(isolines,null,ls);
        }
        CommandLineUtils.reportProgressEnd();

        System.out.println(" Creating new container ");
        IsolineContainer new_cont = new IsolineContainer(gf,isolines);

        System.out.println(" New container size: "+new_cont.size());

        System.out.println(" Serializing new container");

        new_cont.serialize(outPath);

        GsonBuilder gb = new GsonBuilder();
        gb.setPrettyPrinting();
        gb.registerTypeAdapter(LineString.class,new IsolineContainer.LineStringAdapter(new_cont.getFactory()));

        String cutted_lines = gb.create().toJson(cuttedLines);

        try {
            PrintWriter writer = new PrintWriter(outPath.substring(0,outPath.lastIndexOf(".json"))+"_cutted_lines.json", "UTF-8");
            writer.println(cutted_lines);
            writer.close();
        } catch (Exception ex) {
            CommandLineUtils.printWarning("could not serialize Isoline Container, reason: "+ex.getMessage());
        }


        CommandLineUtils.report();
    }

    public static double getStd(Collection<Double> values) {
        if(values.size() == 0)
            return -1;
        double sum = 0;
        double sq_sum = 0;
        for(Double val : values) {
            sum += val;
            sq_sum += val * val;
        }
        double mean = sum / values.size();
        double variance = sq_sum / values.size() - mean * mean;
        double ret = Math.sqrt(variance);
        if (Double.isNaN(ret)) return -1;
        return ret;
    }

    public static double getMedian(Collection<Double> values) {
        ArrayList<Double> newArray = new ArrayList<>(values);
        newArray.sort((lhs,rhs)->Double.compare(lhs,rhs));
        if (values.size() == 0) return -1;
        double median;
        if (newArray.size() % 2 == 0)
            median = ((double)newArray.get(newArray.size()/2) + (double)newArray.get(newArray.size()/2 - 1))/2;
        else
            median = (double) newArray.get(newArray.size()/2);
        return median;
    }


    public static boolean isValid(LineSegment ls, ArrayList<LineString> strings, double threshold) {
        for (LineString string : strings) {
            Coordinate c1 =  string.getCoordinateN(0);
            Coordinate c2 = string.getCoordinateN(string.getNumPoints()-1);

            if (c1.distance(ls.p0) < threshold && c2.distance(ls.p1) < threshold) return true;
            if (c1.distance(ls.p1) < threshold && c2.distance(ls.p0) < threshold) return true;
        }
        return false;
    }


//    public static sourceDataRes gatherSourceData(Connection con, ArrayList<Connection> cons) {
//
//        sourceDataRes res = new sourceDataRes();
//        ArrayList<Double> scores = new ArrayList<>();
//        ArrayList<Double> intersecting_scores = new ArrayList<>();
//        for (Connection con2 : cons) {
//            if ((con2.first()   == con.first() && con2.second() == con.second()) ||
//                (con2.second()  == con.first() && con2.first()  == con.second()))
//                continue;
//            if (con2.first()    == con.first() || con2.first()     == con.second() ||
//                con2.second()   == con.first() || con2.second()    == con.second()) {
//                res.connections_count += 1;
//                scores.add( getDraftScore(con2) );
//
//            }
//
//            if (Tracer.intersects(con.getConnectionSegment(),con2.getConnectionSegment(),0.01,0.99)) {
//                intersecting_scores.add(getDraftScore(con2));
//            }
//        }
//        if (scores.size() > 0) {
//            res.max_score = scores.stream().max(Double::compare).get();
//            res.min_score = scores.stream().min(Double::compare).get();
//            res.mean_score = scores.stream().reduce((lhs, rhs) -> lhs + rhs).get() / scores.size();
//            if (res.max_score < getDraftScore(con)) res.isMax = true;
//        }
//
//        if (intersecting_scores.size() > 0) {
//            res.intersecting_max_score = intersecting_scores.stream().max(Double::compare).get();
//            res.intersecting_min_score = intersecting_scores.stream().min(Double::compare).get();
//            res.intersecting_mean_score = intersecting_scores.stream().reduce((lhs, rhs) -> lhs + rhs).get() / intersecting_scores.size();
//            if (res.intersecting_max_score < getDraftScore(con)) res.intersecting_isMax = true;
//        }
//        return res;
//    }

    public static void applyNoize() {

    }

    public static void generateData(IsolineContainer input_cont, String output_path) {

        IsolineContainer cont;
        ArrayList<LineString> cutted_lines;
        {
            Pair<ArrayList<LineString>,IsolineContainer> res = genData(input_cont);
            cont = res.second();
            cutted_lines = res.first();

            String extension = OutputUtils.getExtension(output_path);

            cont.serialize(output_path.substring(0,output_path.length()-extension.length()-1)+"_map.json");
        }

        GeometryFactory gf = cont.getFactory();
        CachedTracer<Geometry> intersector = new CachedTracer<>(cont.stream().map(IIsoline::getGeometry).collect(Collectors.toList()),(x)->x, gf);
                RandomForestEvaluator rf_eval = new RandomForestEvaluator(intersector);

        ArrayList<Algorithm.LineConnection.Isoline_attributed> isos = new ArrayList<>(cont.size());
        for (IIsoline i : cont)
            isos.add(new Algorithm.LineConnection.Isoline_attributed(i));

        ArrayList<RandomForestEvaluator.Connection_attributed> cons = rf_eval.getConnectionsTrain(isos,cont.getFactory());

        PrintWriter writer;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output_path), "utf-8"));
        } catch (Exception ex) {
            throw new RuntimeException("Could not write to "+output_path);
        }

        CommandLineUtils.reportProgressBegin("Writing connections dataset to \'"+output_path+"\'");
        int counter = 0;


        writer.write("Y,x1,y1,x2,y2,");
        for (int fi = 0; fi != RandomForestEvaluator.feature_names.length; ++fi) {
            writer.print(RandomForestEvaluator.feature_names[fi]);
            if (fi < RandomForestEvaluator.feature_names.length-1) {
                writer.print(",");
            }
        }
        writer.println();
        int true_count = 0;
        int false_count = 0;
        for (RandomForestEvaluator.Connection_attributed con_atr : cons) {
            Connection con = con_atr.con;
            double[] f = con_atr.features;

            CommandLineUtils.reportProgress(++counter, cons.size());
            if (con.first().isoline.getIsoline().isSteep() ||
                    con.second().isoline.getIsoline().isSteep()) continue;

            int y = (isValid(con.getConnectionSegment(), cutted_lines, 0.000001)) ? 1 : 0;

            //sourceDataRes additional_data = gatherSourceData(con, cons_array);
            writer.print(y+","+
                    con.first().line.p1.x+","+
                    con.first().line.p1.y+","+
                    con.second().line.p1.x+","+
                    con.second().line.p1.y+",");
            for (int fi = 0; fi != f.length;++fi) {
                writer.print(f[fi]);
                if (fi != f.length-1) writer.print(",");
            }
            writer.println();
            if (y == 1) {
                true_count += 1;
            } else {
                false_count += 1;
            }
        }
        CommandLineUtils.reportProgressEnd();

        System.out.println("Class 1 count: " + true_count);
        System.out.println("Class 0 count: " + false_count);
        System.out.println("Class 1 percent: " + ((double) true_count) / (true_count + false_count) * 100);
        System.out.println("Class 0 percent: " + ((double) false_count) / (true_count + false_count) * 100);
        writer.close();
    }

    public static void main(String[] args) {


//
//        SimplexNoise noise = new SimplexNoise(0.01,0,1, 7, 0.5, 123223);
//
//        double[][] result = new double[1024][1024];
//
//        double min = 1000;
//        double max = -1000;
//        for (int x = 0; x != 1024; ++x) {
//            for (int y = 0; y != 1024; ++y) {
//                double n = noise.getNoise((float) x / 1024, (float) y / 1024);
//                min = Double.min(min, n);
//                max = Double.max(max, n);
//                result[x][y] = n;
//            }
//        }

        //System.out.println("["+min+", "+max+"],\\");


        //RasterUtils.saveAsPng(result,"noise_test.png");

        if (true) return;

        int data_gen_size = 1;
        boolean skip_gendata = false;

        for (int i = 0; i != data_gen_size; ++i) {

            System.out.println("Iteration "+i+"...");

            if (!skip_gendata) genData("data_gen"+i+".json");

            IsolineContainer cutted_map;
            try {
                cutted_map = IsolineContainer.deserialize("data_gen"+i+".json");
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }

            byte[] encoded;
            try {
                encoded = Files.readAllBytes(Paths.get("data_gen"+i+"_cutted_lines.json"));
            } catch (Exception ex) {
                throw new RuntimeException("Fail!");
            }
            String json_str = new String(encoded, StandardCharsets.UTF_8);

            GsonBuilder gb = new GsonBuilder();
            gb.registerTypeAdapter(LineString.class, new IsolineContainer.LineStringAdapter(cutted_map.getFactory()));
            Gson gson = gb.create();
            TypeToken<ArrayList<LineString>> token = new TypeToken<ArrayList<LineString>>() {
            };
            ArrayList<LineString> cutted_lines = gson.fromJson(json_str, token.getType());

            //ArrayList<LineString> cutted_lines = gson.fromJson(json_str,new ArrayList<LineString>().getClass() );


            // Get connection scores
            System.out.println();
            System.out.println("Gathering connection dataset...");
            GeometryFactory gf = cutted_map.getFactory();

            ArrayList<Algorithm.LineConnection.Isoline_attributed> isos = new ArrayList<>(cutted_map.size());

            for (IIsoline cmap : cutted_map) {
                Algorithm.LineConnection.Isoline_attributed i_attr = new Algorithm.LineConnection.Isoline_attributed(cmap);
                isos.add(i_attr);
            }


            ArrayList<LineString> steeps = new ArrayList<>();
            for (IIsoline cmap : cutted_map) {
                if (cmap.getType() == 4) {
                    steeps.add(cmap.getLineString());
                }
            }

            ConnectionEvaluator eval = new ConnectionEvaluator(
                    Math.toRadians(Constants.CONNECTIONS_MIN_ANGLE_DEG),
                    Math.toRadians(Constants.CONNECTIONS_MAX_ANGLE_DEG),
                    Constants.CONNECTIONS_MAX_DIST,
                    Constants.CONNECTIONS_WELD_DIST
            );
            //MapEdge mapEdge = MapEdge.fromIsolines(cutted_map, Constants.EDGE_CONCAVE_THRESHOLD);

            CachedTracer<Geometry> intersector = new CachedTracer<Geometry>(isos.stream().map((x) -> x.getGeometry()).collect(Collectors.toList()),(x)->x, gf);
            //SteepDetector steepDetector = new SteepDetector(steeps, Constants.CONNECTIONS_NEAR_STEEP_THRESHOLD, gf);


            RandomForestEvaluator rf_eval = new RandomForestEvaluator(intersector);

            ArrayList<Pair<Connection,double[]>> features = rf_eval.getFeatures(isos, gf);

            //ConnectionExtractor extr = new ConnectionExtractor(intersector, steepDetector, eval, gf, mapEdge);

            //ArrayList<Connection> cons_array = extr.applyAdvanced(isos);

            System.out.println("Gathered " + features.size() + " connections");

            PrintWriter writer;
            try {
                writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("connection_dataset"+i+".csv"), "utf-8"));
            } catch (Exception ex) {
                throw new RuntimeException("Could not write to connection_dataset"+i+".csv");
            }

            CommandLineUtils.reportProgressBegin("Writing connections dataset to \'connection_dataset.csv\"");
            int counter = 0;


            writer.write("Y,x1,y1,x2,y2");
            for (int fi = 0; fi != RandomForestEvaluator.feature_names.length; ++fi) {
                writer.print(RandomForestEvaluator.feature_names[fi]);
                if (fi < RandomForestEvaluator.feature_names.length-1) {
                    writer.print(",");
                }
            }
            writer.println();
            int true_count = 0;
            int false_count = 0;
            for (Pair<Connection,double[]> cf_pair : features) {
                Connection con = cf_pair.v1;
                double[] f = cf_pair.v2;

                CommandLineUtils.reportProgress(++counter, features.size());
                if (con.first().isoline.getIsoline().isSteep() ||
                        con.second().isoline.getIsoline().isSteep()) continue;

                int y = (isValid(con.getConnectionSegment(), cutted_lines, 0.000001)) ? 1 : 0;

                //sourceDataRes additional_data = gatherSourceData(con, cons_array);
                writer.print(y+","+
                        con.first().line.p1.x+","+
                        con.first().line.p1.y+","+
                        con.second().line.p1.x+","+
                        con.second().line.p1.y+",");
                for (int fi = 0; fi != f.length;++fi) {
                    writer.print(f[fi]);
                    if (fi != f.length-1) writer.print(",");
                }
                writer.println();
                if (y == 1) {
                    true_count += 1;
                } else {
                    false_count += 1;
                }
            }
            CommandLineUtils.reportProgressEnd();

            System.out.println("Class 1 count: " + true_count);
            System.out.println("Class 0 count: " + false_count);
            System.out.println("Class 1 percent: " + ((double) true_count) / (true_count + false_count) * 100);
            System.out.println("Class 0 percent: " + ((double) false_count) / (true_count + false_count) * 100);
            writer.close();


            // Get nearby scores
            System.out.println();
            System.out.println("Gathering nearby dataset...");
            NearbyEstimator est = new NearbyEstimator(gf);
            NearbyContainer cont = new NearbyContainer(cutted_map);
            NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));
            SimpleWeightedGraph<Isoline_attributed.LineSide, NearbyEstimator.EdgeAttributed> g = graph.getGraph();


            try {
                writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("nearby_dataset"+i+".csv"), "utf-8"));
            } catch (Exception ex) {
                throw new RuntimeException("Could not write to nearby_dataset"+i+".csv");
            }

            writer.println("Y,min_length,max_length,type_match,count,std,median");
            CommandLineUtils.reportProgressBegin("Writing nearby dataset to file \"nearby_dataset"+i+".csv\"");
            int total = g.vertexSet().size();
            counter = 0;
            for (Isoline_attributed.LineSide side1 : g.vertexSet()) {
                counter += 1;
                CommandLineUtils.reportProgress(counter, total);
                for (Isoline_attributed.LineSide side2 : g.vertexSet()) {
                    if (side1 == side2) continue;
                    NearbyEstimator.EdgeAttributed edge = g.getEdge(side1, side2);
                    double y = Math.abs(side1.getIsoline().getIsoline().getHeight() - side2.getIsoline().getIsoline().getHeight()) < 1.1 ? 1 : 0;
                    double type_match = (side1.getIsoline().getIsoline().getType() == side2.getIsoline().getIsoline().getType() ? 1 : 0);
                    double count = 0;
                    double std = -1;
                    double median = -1;
                    double min_length = Math.min(side1.getIsoline().getIsoline().getLineString().getLength(), side2.getIsoline().getIsoline().getLineString().getLength());
                    double max_length = Math.max(side1.getIsoline().getIsoline().getLineString().getLength(), side2.getIsoline().getIsoline().getLineString().getLength());
                    if (edge == null) {
                        continue;
                        //if (y == 1) continue;
//                    count = 0;
//                    std = -1;
//                    median = -1;
                    } else {
                        ArrayList<Double> trace_lengths = edge.getTraces();
                        //if (trace_lengths.size() == 0 && y==1) continue;
                        count = trace_lengths.size();
                        std = getStd(trace_lengths);
                        median = getMedian(trace_lengths);
                    }
                    writer.println(y + "," + min_length + "," + max_length + "," + type_match + "," + count + "," + std + "," + median);
                }
            }
            CommandLineUtils.reportProgressEnd();
            writer.close();
        }
    }



}
