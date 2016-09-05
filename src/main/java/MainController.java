import Algorithm.BuildingIndex.Index;
import Algorithm.DatasetGenerator.Datagen;
import Algorithm.Healing.Healer;
import Algorithm.Interpolation.DistanceFieldInterpolation;
import Algorithm.Interpolation.Triangulation;
import Algorithm.LineConnection.LineWelder;
import Algorithm.LineConnection.MapEdge;
import Algorithm.Mesh.Mesh3D;
import Algorithm.NearbyGraph.NearbyContainer;
import Algorithm.NearbyGraph.NearbyEstimator;
import Algorithm.NearbyGraph.NearbyGraphWrapper;
import Algorithm.Texture.TextureGenerator;
import Deserialization.Interpolation.SlopeMark;
import Deserialization.DeserializedOCAD;
import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.*;
import com.sun.javafx.scene.shape.PathUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public void openFile(File f) throws Exception {
        deserializedOCAD = new DeserializedOCAD();

        isolineContainer = new IsolineContainer(gf);
        String f_path = f.getPath();
        String configPath = f_path.substring(0, f_path.length()-OutputUtils.getExtension(f_path).length())+"ini";
        deserializedOCAD.DeserializeMap(f_path,configPath);
        ArrayList<IIsoline> isos = deserializedOCAD.toIsolines(1,gf);
        slopeMarks = new ArrayList<>();
        isos.forEach(isolineContainer::add);
        deserializedOCAD.slopeMarks.forEach(slopeMarks::add);
        System.out.println(("Added " + IsolineCount() + " isolines, bounding box: " + isolineContainer.getEnvelope()));
        CommandLineUtils.report();
    }

    public void openJsonFile(File f) throws Exception {
        isolineContainer = IsolineContainer.deserialize(f.getAbsolutePath());
        CommandLineUtils.report();
    }

    public void saveJsonFile(File f) throws Exception {
        isolineContainer.serialize(f.getAbsolutePath());
        CommandLineUtils.report();
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

    public void heal() {

        Healer.heal(isolineContainer,isolineContainer.getFactory());
    }

    public void connectLines() {
        if (edge == null) return;
        LineWelder lw = new LineWelder(gf,edge);
        isolineContainer = new IsolineContainer(gf,lw.WeldAll(isolineContainer));
        CommandLineUtils.report();
    }

//    public void detectEdge() {
//        edge = MapEdge.fromIsolines(isolineContainer, Constants.EDGE_CONCAVE_THRESHOLD);
//
//        CommandLineUtils.report();
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
//        CommandLineUtils.report("Graph was built successfully");
//        CommandLineUtils.report();
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
//        CommandLineUtils.report();
//    }

    public void buildIndex(){
        index = triangulation == null ? new Index("sample.obj") : new Index(triangulation);
        CommandLineUtils.report();
        //index.diamondRain();
    }

    public void generateTexture(String output_path) {

        if (deserializedOCAD != null)
            mesh.generateTexture(deserializedOCAD,output_path,"png");

    }

    private Mesh3D mesh = null;

    private boolean heights_calculated = false;
    public void saveMesh(String output_name) {

        if (heights_calculated == false) {
            NearbyContainer cont = new NearbyContainer(isolineContainer);
            NearbyEstimator est = new NearbyEstimator(isolineContainer.getFactory());
            NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));
            graph.SetHillsSlopeSides();
            graph.ConvertToSpanningTree();
            graph.recoverAllSlopes();
            graph.recoverAllHeights();
            heights_calculated = true;
        }

        mesh = Mesh3D.fromIsolineContainer(isolineContainer);
        mesh.saveAsFbx(output_name);

    }

}
