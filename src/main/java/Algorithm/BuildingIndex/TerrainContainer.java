package Algorithm.BuildingIndex;

import Utils.DebugUtils;
import com.obj.Group;
import com.obj.WavefrontObject;
import jdk.nashorn.internal.ir.Terminal;
import objimp.ObjImpMesh;
import objimp.ObjImpScene;
import toxi.geom.mesh.*;

import java.io.*;

/**
 * Created by fdl on 8/2/16.
 */
public class TerrainContainer {
    WETriangleMesh mesh;
    QTree tree;


    public TerrainContainer(double[][] heightmap) {
        if (DebugUtils.skipExternalSimplification) {
            System.err.println("SKIPPED ./simplify CALL!!! Old data will be used.");
        } else {
            Terrain terrain = new Terrain(heightmap[0].length, heightmap.length, 1);

            int y_steps = heightmap.length;
            int x_steps = heightmap[0].length;
            for (int i = y_steps - 1; i >= 0; --i) {
                for (int j = 0; j != x_steps; ++j) {
                    terrain.setHeightAtCell(j, i, (float) heightmap[i][j]);
                }
            }

            TriangleMesh tempMesh = (TriangleMesh) terrain.toMesh();
            try {
                OutputStream os = new FileOutputStream("mesh.big.obj");
                tempMesh.saveAsOBJ(os);
                tempMesh.clear();
            } catch (FileNotFoundException e) {
                throw new RuntimeException("couldn't write mesh", e);
            }

            try {
                System.out.println("invoking external mesh simplifier, ETA 25 seconds...");
                Process p = new ProcessBuilder("./simplify", "mesh.big.obj", "mesh.comp.obj", "0.05").inheritIO().start();
                if (p.waitFor() != 0) throw new RuntimeException("External simplification failed");
            } catch (IOException e) {
                throw new RuntimeException("Problems while invoking simplifier", e);
            } catch (InterruptedException ie) {
                throw new RuntimeException("interrupted while simplifying mesh", ie);
            }
        }

        mesh = OBJReLoader.load("mesh.comp.obj");
        tree = new QTree(Box.createBox(mesh));

        for (Face face : mesh.faces) {
            tree.add(new WrappedTriangle(face));
        }

        System.out.print(tree);
    }


}
