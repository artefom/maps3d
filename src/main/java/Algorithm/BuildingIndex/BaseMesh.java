package Algorithm.BuildingIndex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by fdl on 8/9/16.
 */
public class BaseMesh extends Mesh {
    public BaseMesh(String objFileName) {
        super(objFileName);
    }

    private static void exportHeightmapToSimplifier(double[][] heightmap, double step, String tempFileName) {
        String bigTempFileName = tempFileName + ".in";
        try {
            BufferedWriter obj = new BufferedWriter(new FileWriter(bigTempFileName));
            int rows = heightmap.length, cols = heightmap[0].length, i = 1;
            double z = 0;
            for (int r = 0; r < rows; ++r, z += step) {
                double x = 0;
                for (int c = 0; c < cols; ++c, x += step, ++i) {
                    printVertex(x, heightmap[r][c], z, obj);
                    if (c > 0 && r > 0) {
                        printFace(i, i - cols, i - cols - 1, obj);
                        printFace(i, i - cols - 1, i - 1, obj);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't write obj, which is needed to call external simplifier. Btw writing to: " + bigTempFileName, e);
        }

        try {
            System.out.println("invoking external mesh simplifier, ETA 25 seconds...");
            //invokes binary from https://github.com/sp4cerat/Fast-Quadric-Mesh-Simplification
            Process p = new ProcessBuilder(
                    System.getProperty("os.name").toLowerCase().startsWith("win") ? "simplify.exe" : "./simplify",
                    bigTempFileName, tempFileName, "0.02").inheritIO().start();
            if (p.waitFor() != 0) throw new RuntimeException("External simplification failed!");
        } catch (IOException e) {
            throw new RuntimeException("problems while invoking simplifier", e);
        } catch (InterruptedException ie) {
            throw new RuntimeException("interrupted while simplifying mesh", ie);
        }
    }

    public static BaseMesh generateByHeightmap(double[][] heightmap, double step, String tempFileName) {
        exportHeightmapToSimplifier(heightmap, step, tempFileName);
        return new BaseMesh(tempFileName);
    }

    @Override
    public void move(double dx, double dy, double dz) {
        throw new RuntimeException("Can't move BaseMesh, cause QTree is already assigned to it");
    }
}
