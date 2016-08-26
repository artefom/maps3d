package Algorithm.BuildingIndex;

import Algorithm.Interpolation.Triangulation;
import com.vividsolutions.jts.geom.*;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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
    private static char[] V = {'v', ' '}, F = {'f', ' '};

    static void printVertex(double x, double y, double z, BufferedWriter obj) throws IOException {
        obj.write(V);
        obj.write(Double.toString(x));
        obj.write(' ');
        obj.write(Double.toString(y));
        obj.write(' ');
        obj.write(Double.toString(z));
        obj.newLine();
    }

    static void printFace(int v1, int v2, int v3, BufferedWriter obj) throws IOException {
        obj.write(F);
        obj.write(Integer.toString(v1));
        obj.write(' ');
        obj.write(Integer.toString(v2));
        obj.write(' ');
        obj.write(Integer.toString(v3));
        obj.newLine();
    }

    protected Mesh(){}

    Mesh(String objFileName) {
        try {
            Scanner obj = new Scanner(new FileReader(objFileName));
            String token;
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
                        Triplet face = new Triplet(obj.nextInt() - 1, obj.nextInt() - 1, obj.nextInt() - 1);
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
                        System.err.println("Designed to work only with sp4cerat's simplifier, skipping '" + token + " " + line + "'");
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
                printVertex(coordinate.x, vertexesY.get(i), coordinate.y, obj);
            }
            for (Triplet face : faceIndices) {
                printFace(face.a + 1, face.b + 1, face.c + 1, obj);
            }
            obj.close();
        } catch (IOException e) {
            throw new RuntimeException("Can't export obj with name " + fileName, e);
        }

    }
}




















