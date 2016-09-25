package Algorithm.BuildingIndex;

import Algorithm.Interpolation.Triangulation;
import Utils.GeomUtils;
import Utils.TriangleUtils;
import com.vividsolutions.jts.geom.*;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by fdl on 8/8/16.
 */
public class Mesh {
    public class Triplet {
        public final int a, b, c;

        public Triplet(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public Triplet(int[] triplet) {
            a = triplet[0];
            b = triplet[1];
            c = triplet[2];
        }

        public Triplet createIncreasedBy(int delta) {
            return new Triplet(a + delta, b + delta, c + delta);
        }

        @Override
        public String toString(){
            return "(" + a + "," + b + "," + c + ")";
        }
    }

    public final ArrayList<Coordinate> vertexesXZ = new ArrayList<>();
    public final ArrayList<Double> vertexesY = new ArrayList<>();
    public final ArrayList<Polygon> trianglesXZ = new ArrayList<>();
    public final ArrayList<Triplet> faceIndices = new ArrayList<>();
    /**
     * boxXZ is guaranteed to be valid only after construction time. Represents coverage area of initial structure.
     */
    public final Box boxXZ = new Box();
    protected int initialVertexesCount, getInitialFacesCount;

    protected static GeometryFactory gf = new GeometryFactory();

    private static void printVertex(double x, double y, double z, String beginning, char separator, String ending, BufferedWriter obj) throws IOException {
        obj.write(beginning);
        obj.write(Double.toString(x));
        obj.write(separator);
        obj.write(Double.toString(y));
        obj.write(separator);
        obj.write(Double.toString(z));
        obj.write(ending);
    }

    private static void printFace(int v1, int v2, int v3, String beginning, char separator, String ending, BufferedWriter obj) throws IOException {
        obj.write(beginning);
        obj.write(Integer.toString(v1));
        obj.write(separator);
        obj.write(Integer.toString(v2));
        obj.write(separator);
        obj.write(Integer.toString(v3));
        obj.write(ending);
    }

    protected Mesh(){}

    Mesh(String objFileName) {
        try {
            Scanner obj = new Scanner(new FileReader(objFileName));
            String token, lastSkipped = "v";
            int cntSkipped = 0;
            double x, y, z;
            while (obj.hasNext()) {
                switch (token = obj.next()) {
                    case "v":
                        x = obj.nextDouble();
                        y = obj.nextDouble();
                        z = obj.nextDouble();
                        boxXZ.update(x,z);
                        vertexesXZ.add(new Coordinate(x, z));
                        vertexesY.add(y);
                        break;
                    case "f":
                        String[] numbers = obj.nextLine().split("\\s+");
                        if (numbers[1].contains("/")) {
                            for (int i = 1; i < 4; ++i) {
                                numbers[i] = numbers[i].split("/")[0];
                            }
                        }
                        Triplet face = new Triplet(Integer.parseInt(numbers[1]) - 1, Integer.parseInt(numbers[2]) - 1, Integer.parseInt(numbers[3]) - 1);
                        if (face.a * face.b * face.c == 0) {
                            System.out.println("zero-index is synced");
                        }
                        faceIndices.add(face);
                        Polygon polygon = gf.createPolygon(new Coordinate[]{
                                vertexesXZ.get(face.a),
                                vertexesXZ.get(face.b),
                                vertexesXZ.get(face.c),
                                vertexesXZ.get(face.a)
                        });
                        polygon.setUserData(trianglesXZ.size());
                        trianglesXZ.add(polygon);
                        break;
                    default:
                        String line = obj.nextLine();
                        if (!token.equals(lastSkipped)) {
                            if (cntSkipped != 0) System.err.println("\t//+" + cntSkipped + " time(-s)");
                            System.err.println("Supporting only v %f %f %f and f %d[/%d] %d[/%d] %d[/%d] strings. Skipping '" + token + " " + line + "'");
                            lastSkipped = token;
                            cntSkipped = 0;
                        } else {
                            ++cntSkipped;
                        }
                        //throw new IOException("Unexpected token '" + token + "'");
                }
            }
            obj.close();
            boxXZ.acceptUpdates();
            initialVertexesCount = vertexesXZ.size();
            getInitialFacesCount = faceIndices.size();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read output of external simplifier.", e);
        }
    }

    public void add(Mesh meshToAdd) {
        int delta = vertexesY.size();
        vertexesXZ.addAll(meshToAdd.vertexesXZ); //todo copy instances
        vertexesY.addAll(meshToAdd.vertexesY); //todo copy instances
        trianglesXZ.addAll(meshToAdd.trianglesXZ); //todo disrequire triangles for non-base meshes
        meshToAdd.faceIndices.forEach(face -> faceIndices.add(face.createIncreasedBy(delta)));
        faceIndices.addAll(meshToAdd.faceIndices);
    }

    public void move(double dx, double dy, double dz) {
        if (dx != 0 || dz != 0) {
            for (int i = 0; i < vertexesXZ.size(); ++i) {
                Coordinate c = vertexesXZ.get(i);
                vertexesXZ.set(i, new Coordinate(c.x + dx, c.y + dz));
            }
            CoordinateSequenceFilter csf = new CoordinateSequenceFilter() {
                private boolean isDone = false;

                @Override
                public void filter(CoordinateSequence seq, int i) {
                    seq.setOrdinate(i, 0, seq.getOrdinate(i,0) + dx);
                    seq.setOrdinate(i, 1, seq.getOrdinate(i,1) + dz);
                    if (i == seq.size()-1) isDone = true;
                }

                @Override
                public boolean isDone() {
                    return isDone;
                }

                @Override
                public boolean isGeometryChanged() {
                    return true;
                }
            };

            trianglesXZ.forEach(t -> t.apply(csf));
        }
        if (dy != 0) {
            for (int i = 0; i < vertexesY.size(); i++) {
                vertexesY.set(i, vertexesY.get(i) + dy);
            }
        }
    }

    public void saveAsObj(String fileName){
        try {
            BufferedWriter obj = new BufferedWriter(new FileWriter(fileName));
            for (int i = 0; i < vertexesY.size(); ++i) {
                Coordinate coordinate = vertexesXZ.get(i);
                printVertex(coordinate.x, vertexesY.get(i), coordinate.y, "v ", ' ', "\n", obj);
            }
            for (Triplet face : faceIndices) {
                printFace(face.a + 1, face.b + 1, face.c + 1, "f ", ' ', "\n", obj);
            }
            obj.close();
        } catch (IOException e) {
            throw new RuntimeException("Can't export obj with typename " + fileName, e);
        }
    }

    private double scale = 1., dx = 0., dz = 0.;

    public void dumpToJS(BufferedWriter bw){
        try {
            bw.write("dx:" + dx + ",\n");
            bw.write("dz:" + dz + ",\n");
            bw.write("scaling: " + scale + ",\n");
            bw.write("vertexes: [\n");
            for (int i = 0; i < vertexesY.size(); ++i) {
                Coordinate coordinate = vertexesXZ.get(i);
                printVertex(coordinate.x, vertexesY.get(i), coordinate.y, "[", ',',
                        i == vertexesY.size()-1 ? "]" : "],",
                        bw);
            }
            bw.write("\n],triplets: [\n");
            for (int i = 0; i < faceIndices.size(); ++i) {
                Triplet face = faceIndices.get(i);
                printFace(face.a, face.b, face.c, "[", ',',
                        i == faceIndices.size()-1 ? "]" : "],",
                        bw);
            }
            bw.write("\n]");
        } catch (IOException e) {
            throw new RuntimeException("Can't export to js", e);
        }
    }

    public double centerAndNormalize(){
        dx = -(boxXZ.x0+boxXZ.x1)/2;
        dz = -(boxXZ.z0+boxXZ.z1)/2;
        double inv_scale = Math.max(boxXZ.xsize(), boxXZ.zsize()), full_scale = 2/inv_scale;
        double htx = boxXZ.xsize()/inv_scale, htz = boxXZ.zsize()/inv_scale;

        Consumer<Coordinate> tfm = c -> {
            c.x = GeomUtils.map(c.x, boxXZ.x0, boxXZ.x1, -htx, htx);
            c.y = GeomUtils.map(c.y, boxXZ.z0, boxXZ.z1, -htz, htz);
        };

        vertexesXZ.forEach(tfm);

        for (int i = 0; i < vertexesXZ.size(); ++i) {
            vertexesY.set(i, vertexesY.get(i)*full_scale);
        }

        boxXZ.apply(tfm);

        this.scale = full_scale;
        System.out.println("scaled by " + full_scale);
        return full_scale;
    }

}




















