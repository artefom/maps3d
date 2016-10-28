package ru.ogpscenter.maps3d;

import Deserialization.DeserializedOCAD;
import Deserialization.Interpolation.SlopeMark;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import ru.ogpscenter.maps3d.algorithm.NearbyGraph.NearbyContainer;
import ru.ogpscenter.maps3d.algorithm.NearbyGraph.NearbyEstimator;
import ru.ogpscenter.maps3d.algorithm.NearbyGraph.NearbyGraphWrapper;
import ru.ogpscenter.maps3d.algorithm.healing.Healer;
import ru.ogpscenter.maps3d.algorithm.index.Index;
import ru.ogpscenter.maps3d.algorithm.interpolation.DistanceFieldInterpolation;
import ru.ogpscenter.maps3d.algorithm.interpolation.Triangulation;
import ru.ogpscenter.maps3d.algorithm.mesh.Mesh3D;
import ru.ogpscenter.maps3d.algorithm.repair.LineWelder;
import ru.ogpscenter.maps3d.algorithm.repair.MapEdge;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;
import ru.ogpscenter.maps3d.utils.OutputUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Created by Artyom.Fomenko on 15.07.2016.
 * Parsing input, providing output, managing dataflows between classes
 */
public class MainController {

    private GeometryFactory gf;

    public IsolineContainer isolineContainer;
    public DistanceFieldInterpolation interpolation;
    public Triangulation triangulation;
    public Index index;
    DeserializedOCAD deserializedOCAD;

    public MapEdge edge;

    public ArrayList<SlopeMark> slopeMarks;

    MainController() {
        gf = new GeometryFactory();
        isolineContainer = new IsolineContainer(gf);
        edge = null;
    }

//    public void openFile(File f) throws IOException {
//        FileInputStream fis = new FileInputStream(f);
//        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//
//        String line = null;
//        int id = 0;
//        while ((line = br.readLine()) != null) {
//            String[] tokens = line.split("[\\s]+");
//            int type = Integer.parseInt(tokens[0]);
//            int side = Integer.parseInt(tokens[1]);
//            int n = Integer.parseInt(tokens[2]);
//
//            Coordinate[] carr = new Coordinate[n];
//            for (int i = 0; i < n; i+=1) {
//                double x = Double.parseDouble(tokens[3+i*2]);
//                double y = Double.parseDouble(tokens[3+i*2+1]);
//                carr[i] = new Coordinate(x,y);
//            }
//            CoordinateSequence cs = gf.getCoordinateSequenceFactory().create(carr);
//            Isoline is = new Isoline( type, side, cs, gf);
//            id += 1;
//            isolineContainer.add( is );
//        }
//    }

    public DeserializedOCAD openFile(File ocadFile, BiConsumer<Integer, Integer> progressUpdate) throws Exception {
        deserializedOCAD = new DeserializedOCAD();
        isolineContainer = new IsolineContainer(gf);
        deserializedOCAD.loadOcad(ocadFile, progressUpdate);
        ArrayList<IIsoline> isolines = deserializedOCAD.toIsolines(gf);
        slopeMarks = new ArrayList<>();
        isolines.forEach(isolineContainer::add);
        deserializedOCAD.slopeMarks.forEach(slopeMarks::add);
        System.out.println(("Added " + IsolineCount() + " isolines, bounding box: " + isolineContainer.getEnvelope()));
        CommandLineUtils.reportFinish();
        return deserializedOCAD;
    }

    public void openJsonFile(File f) throws Exception {
        isolineContainer = IsolineContainer.deserialize(f.getAbsolutePath());
        CommandLineUtils.reportFinish();
    }

    public void saveJsonFile(File f) throws Exception {
        isolineContainer.serialize(f.getAbsolutePath());
        CommandLineUtils.reportFinish();
    }

    public int IsolineCount() {
        return isolineContainer.size();
    }

    public Stream<IIsoline> getIsolinesInCircle(double x, double y, double radius, IsolineContainer ic) {
        Point p = gf.createPoint( new Coordinate(x,y) );
        return ic.stream().filter((il)->
                il.getGeometry().isWithinDistance(p,radius)
        );
    }

    public void heal(BiConsumer<Integer, Integer> progressUpdate) {
        Healer.heal(isolineContainer,isolineContainer.getFactory(), progressUpdate);
    }

    public void connectLines(BiConsumer<Integer, Integer> progressUpdate) {
        if (edge == null) return;
        LineWelder lw = new LineWelder(gf,edge);
        LinkedList<IIsoline> isolines = lw.weldAll(isolineContainer, progressUpdate);
        isolineContainer = new IsolineContainer(gf, isolines);
        CommandLineUtils.reportFinish();
    }

//    public void detectEdge() {
//        edge = MapEdge.fromIsolines(isolineContainer, Constants.EDGE_CONCAVE_THRESHOLD);
//
//        CommandLineUtils.reportFinish();
//    }

//    public void buildGraph() {
//        NearbyContainer cont = new NearbyContainer(isolineContainer);
//        NearbyEstimator est = new NearbyEstimator(gf);
//        NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));
//        graph.SetHillsSlopeSides();
//        graph.ConvertToSpanningTree();
//        graph.recoverAllSlopes();
//        graph.recoverAllHeights();
//        //isolineContainer.serialize("cached_isolines.json");
//
//        CommandLineUtils.reportFinish("Graph was built successfully");
//        CommandLineUtils.reportFinish();
//    }
//
//    double [][] heightmap = null;
//    public void interpolate(String name) {
//        interpolation = new DistanceFieldInterpolation(isolineContainer);
//        heightmap = interpolation.getAllInterpolatingPoints();
//
//        //OutputUtils.saveAsTXT(heightmap);
//        //OutputUtils.saveAsPNG(heightmap);
//
//        System.out.println("Dumping png...");
//        RasterUtils.saveAsPng(heightmap,name);
//
//        System.out.println("Dumping txt...");
//        RasterUtils.saveAsTxt(heightmap,name);
//
//        triangulation = new Triangulation(heightmap);
//        triangulation.writeToFile(name);
//        CommandLineUtils.reportFinish();
//    }

    public void buildIndex(){
//        index = triangulation == null ? new Index("sample.obj") : new Index(triangulation);
        throw new RuntimeException("Not implemented");
//        CommandLineUtils.reportFinish();
        //index.diamondRain();
    }

    public boolean generateTexture(String output_path, BiConsumer<Integer, Integer> progressUpdate) {

        if (deserializedOCAD != null) {
            generateTexture(output_path,deserializedOCAD, progressUpdate);
            return true;
        }

        return false;
    }


    public boolean generateTexture(String output_path, DeserializedOCAD ocad, BiConsumer<Integer, Integer> progressUpdate) {

        if (ocad != null) {
            String extension = OutputUtils.getExtension(output_path);
            if (extension.length() > 0) {
                output_path = output_path.substring(0,output_path.length()-1-extension.length());
                extension = "png";
            } else {
                extension = "png";
            }
            getMesh(progressUpdate).generateTexture(ocad, output_path, extension);
            return true;
        }

        return false;
    }

    private Mesh3D mesh_cache = null;

    private boolean heights_calculated = false;

    private Mesh3D getMesh(BiConsumer<Integer, Integer> progressUpdate) {
        // todo(MS): update progress
        if (mesh_cache != null) return mesh_cache;
        if (!heights_calculated) {
            NearbyContainer cont = new NearbyContainer(isolineContainer);
            NearbyEstimator est = new NearbyEstimator(isolineContainer.getFactory());
            NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));
            graph.SetHillsSlopeSides();
            graph.ConvertToSpanningTree();
            graph.recoverAllSlopes();
            graph.recoverAllHeights();
            heights_calculated = true;
        }
        mesh_cache = Mesh3D.fromIsolineContainer(isolineContainer);
        return mesh_cache;
    }

    public void saveMesh(String output_path, BiConsumer<Integer, Integer> progressUpdate) {

        String extension = OutputUtils.getExtension(output_path);
        if (extension.length() > 0) {
            output_path = output_path.substring(0,output_path.length()-1-extension.length());
        }

        Index index = new Index(getMesh(progressUpdate).saveAsFbx(output_path), true);
        index.dumpToJS("index.js");
    }

}
