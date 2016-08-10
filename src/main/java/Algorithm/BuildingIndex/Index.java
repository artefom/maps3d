package Algorithm.BuildingIndex;

import Utils.DebugUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Created by fdl on 8/2/16.
 */
public class Index {
    private static final GeometryFactory gf = new GeometryFactory();
    BaseMesh mesh;
    QTree tree;

    public Index(double[][] heightmap) {
        if (DebugUtils.skipExternalSimplification) {
            System.err.println("SKIPPED ./simplify CALL!!! Old data will be used.");
            mesh = new BaseMesh("temp.obj");
        } else {
            mesh = BaseMesh.generateByHeightmap(heightmap, 0.25, "temp.obj");
        }
        tree = new QTree(mesh);
        System.out.print(tree);
    }

    public double getHeightAt(double x, double z) {
        return tree.query(new Coordinate(x, z));
    }

    public void addMesh(double x, double y, double z, Mesh meshToAdd) {
        meshToAdd.move(x, y, z);
        mesh.add(meshToAdd);
    }

    public void placeMesh(double x, double z, Mesh meshToAdd) {
        addMesh(x, getHeightAt(x, z), z, meshToAdd);
    }

    public void addDiamond(double x, double z) { //todo replace this with junit test with heightmap : depends on improvement height deterring
        placeMesh(x, z, new Mesh("diamond.obj"));
    }

    public void diamondRain() {
        for (double x = 5; x < 347.5; x += 20) {
            for (double z = 5; z < 451.0; z += 20) {
                try {
                    addDiamond(x, z);
                } catch (RuntimeException re) {
                    System.err.println("QTree FAILED AT " + x + " " + z); //FIXME it seems that external simplifier slightly changes edge of terrain, so getHeightAt(0,0) fails
                    re.printStackTrace();
                }
            }
        }
        this.mesh.saveAsObj("diamond-test.obj");
    }

}
