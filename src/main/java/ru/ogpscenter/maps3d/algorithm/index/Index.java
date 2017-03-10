package ru.ogpscenter.maps3d.algorithm.index;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;
import ru.ogpscenter.maps3d.utils.GeomUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by fdl on 8/2/16.
 */
public class Index {
    private static final GeometryFactory gf = new GeometryFactory();
    final BaseMesh mesh;
    final QTree tree;

    public Index(BaseMesh mesh, boolean centerAndNormalize) {
        this.mesh = mesh;
//        mesh.centerAndNormalize();
        this.tree = new QTree(mesh);
        System.out.print(tree);
    }

//    public Index(String fileName) {
//        this(new BaseMesh(fileName));
//    }
//
//    public Index(Triangulation triangulation) {
//        this(new BaseMesh(triangulation));
//    }

    public double getHeightAt(double x, double z) {
        return tree.query(new Coordinate(x, z));
    }

    public void addMesh(double x, double y, double z, Mesh meshToAdd) {
        meshToAdd.move(x, y, z);
        mesh.add(meshToAdd);
    }

    /**
     * adds specified mesh to contained one by horizontal coordinates
     * @param x
     * @param z
     * @param meshToAdd this mesh will be modified
     * @return height, on which meshToAdd will be raised
     */
    public double placeMesh(double x, double z, Mesh meshToAdd) {
        double h = getHeightAt(x, z);
        if (meshToAdd != null) addMesh(x, h, z, meshToAdd);
        return h;
    }

    public double addDiamond(double x, double z, boolean drawDiamonds) { //todo replace this with junit test with heightmap : depends on improvement height deterring
        try {
            return placeMesh(x, z, drawDiamonds ? new Mesh("diamond.obj") : null);
        } catch (RuntimeException re) {
            System.err.println("QTree FAILED AT " + x + " " + z); //FIXME it seems that external simplifier slightly changes edge of terrain, so getHeightAt(0,0) fails
            re.printStackTrace();
        }
        return Double.NaN;
    }

    public void diamondRain(boolean drawDiamonds) { //todo add test with heightmap. obviously optional
        Box box = tree.getXZBox();
//        for (double x = box.x0; x <= box.x1; x += 0.1)
//            addDiamond(x, box.z0, drawDiamonds);
//        for (double x = box.x1; x >= box.x0; x -= 0.1)
//            addDiamond(x, box.z1, drawDiamonds);
//        for (double z = box.z0; z <= box.z1; z += 0.1)
//            addDiamond(box.x1, z, drawDiamonds);
//        for (double z = box.z1; z >= box.z0; z -= 0.1)
//            addDiamond(box.x0, z, drawDiamonds);
        Random random = new Random(239);
        for (int i = 0; i < 1000; ++i) {
            double x = GeomUtils.map(random.nextDouble(), 0, 1, box.x0, box.x1)/2;
            double z = GeomUtils.map(random.nextDouble(), 0, 1, box.z0, box.z1)/2;
            addDiamond(x, z, drawDiamonds);
        }
        this.mesh.saveAsObj("diamond-test.obj");
        CommandLineUtils.reportFinish("created diamond-test.obj");
    }

    public void dumpToJS(String fileName){
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
            bw.write("{\n");
//            bw.write("var index = {\n");
            mesh.dumpToJS(bw);
            bw.write(",\n");
            tree.dumpToJS(bw);
            bw.write("\n}");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public void dumpToBoxes(String filename){
        try{
            PrintWriter pw = new PrintWriter(filename);
            tree.dumpToBoxes(pw);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public static void main(String[] args) {
        Index index = new Index(new BaseMesh(args[0]), true);
        index.diamondRain(false);
        index.dumpToBoxes(args[0] + ".boxes");
        index.dumpToJS(args[0] + ".js");
        index.mesh.saveAsObj(args[0] + ".out.obj");
    }
}