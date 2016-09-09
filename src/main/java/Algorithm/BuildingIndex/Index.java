package Algorithm.BuildingIndex;

import Algorithm.Interpolation.Triangulation;
import Utils.CommandLineUtils;
import Utils.GeomUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.util.Random;

/**
 * Created by fdl on 8/2/16.
 */
public class Index {
    private static final GeometryFactory gf = new GeometryFactory();
    final BaseMesh mesh;
    final QTree tree;

    private Index(BaseMesh mesh) {
        this.mesh = mesh;
        this.tree = new QTree(mesh);
        System.out.print(tree);
    }

    public Index(String fileName) {
        this(new BaseMesh(fileName));
    }

    public Index(Triangulation triangulation) {
        this(new BaseMesh(triangulation));
    }

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
        addMesh(x, h, z, meshToAdd);
        return h;
    }

    public double addDiamond(double x, double z) { //todo replace this with junit test with heightmap : depends on improvement height deterring
        try {
            return placeMesh(x, z, new Mesh("diamond.obj"));
        } catch (RuntimeException re) {
            System.err.println("QTree FAILED AT " + x + " " + z); //FIXME it seems that external simplifier slightly changes edge of terrain, so getHeightAt(0,0) fails
            re.printStackTrace();
        }
        return Double.NaN;
    }

    public void diamondRain() { //todo add test with heightmap. obviously optional
        Box box = tree.getXZBox();
        for (double x = box.x0; x <= box.x1; x += 20)
            addDiamond(x, box.z0);
        for (double x = box.x1; x >= box.x0; x -= 20)
            addDiamond(x, box.z1);
        for (double z = box.z0; z <= box.z1; z += 20)
            addDiamond(box.x1, z);
        for (double z = box.z1; z >= box.z0; z -= 20)
            addDiamond(box.x0, z);
        Random random = new Random(239);
        for (int i = 0; i < 1000; ++i) {
            double x = GeomUtils.map(random.nextDouble(), 0, 1, box.x0, box.x1);
            double z = GeomUtils.map(random.nextDouble(), 0, 1, box.z0, box.z1);
            addDiamond(x, z);
        }
        this.mesh.saveAsObj("diamond-test.obj");
        CommandLineUtils.report("created diamond-test.obj");
    }

}