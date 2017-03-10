package ru.ogpscenter.maps3d.algorithm.index;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by fdl on 8/4/16.
 */
public class QTree {
    private static final GeometryFactory gf = new GeometryFactory();
    private final int CONTAINMENT_THRESHOLD;
    private final int MAXIMAL_DEPTH;

    public class Node {
        private final Node parent;
        private final int depth;
        private final Box ownBox;

        private boolean isSplit = false;
        private final Node[] quarters = new Node[4];
        private final ArrayList<Polygon> containment = new ArrayList<>();

        private Node(Box ownBox, Node parent, int depth) {
            this.parent = parent;
            this.depth = depth;
            this.ownBox = ownBox;
        }

        private Node(Box ownBox, Node parent) {
            this(ownBox, parent, parent.depth + 1);
        }

        protected Node(Box ownBox) {
            this(ownBox, null, 0);
        }

        private void split() {
            Box[] boxes = ownBox.split();
            for (int i = 0; i < 4; i++) {
                quarters[i] = new Node(boxes[i], this);
            }
            isSplit = true;
        }

        protected void assign(Polygon triangle) {
            if (isSplit) {
                for (Node node : quarters) {
                    if (node.ownBox.intersects(triangle)) {
                        node.assign(triangle);
                    }
                }
//                int qc = 0, lq = 0;
//                for (int i = 0; i < 4; ++i) {
//                    if (quarters[i].ownBox.intersects(wTriangle.triFlat)) {
//                        qc++; lq = i;
//                    }
//                }
                return;
            }
            containment.add(triangle);
            if (depth < MAXIMAL_DEPTH && containment.size() >= CONTAINMENT_THRESHOLD) { //FIXME somewhere over-density should be checked in proper way. Max depth is not ok
                split();
                containment.forEach(this::assign);
                containment.clear();
            }
//            if (depth == MAXIMAL_DEPTH) System.err.println("got maximal depth");
        }

        protected ArrayList<Polygon> query(double x, double z) {
            if (isSplit) {
                for (Node node : quarters) {
                    if (node.ownBox.contains(x, z)) {
                        return node.query(x, z);
                    }
                }
            } else {
                return containment;
            }
            return null;
        }

        @Override
        public String toString() {
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < depth; i++) sb.append(" ");
//            if (isSplit) {
//                sb.append("·\n");
//                assert containment.size() == 0;
//                for (Node node : quarters) sb.append(node.toString());
//            } else {
//                sb.append("•").append(containment.size()).append('\n');
//            }
//            return sb.toString();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) sb.append("  ");
            if (isSplit) {
                assert containment.size() == 0;
                sb.append("splits:\n");
                for (Node node : quarters) sb.append(node.toString());
            } else {
                sb.append("represents: ").append(ownBox).append('\n');
            }
            return sb.toString();
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < depth; i++) sb.append(" · ");
//            sb.append("N-").append(Integer.toHexString(hashCode()));
//            if (isSplit) {
//                sb.append(" is split:\n");
//                assert containment.size() == 0;
//                for (Node node : quarters) sb.append(node.toString());
//            } else {
//                sb.append(" is NOT and contains ").append(containment.size()).append(" faces.");
//            }
//            return sb.toString();
        }

        public void dfd(BufferedWriter bw) throws IOException{
            bw.write('[');
            if (isSplit) {
                bw.write('1');
                for (int i = 0; i < 4; ++i) {
                    bw.write(',');
                    quarters[i].dfd(bw);
                }
            } else {
                bw.write('0');
                for (Polygon face : containment) {
                    bw.write(',');
                    bw.write(((Integer)face.getUserData()).toString());
                }
            }
            bw.write("]\n");
        }

        protected int cnt() {
            int ans = 0;
            if (isSplit) {
                for (Node node : quarters) {
                    ans += node.cnt();
                }
            } else {
                ans += containment.size();
            }
            return ans;
        }

        private void dumpToBoxes(PrintWriter pw){
            if (depth > 10)
                System.err.println("ga");
            if (isSplit)
                for (Node n : quarters) n.dumpToBoxes(pw);
            else
                pw.printf("%f %f %f %f %d\n", ownBox.x0, ownBox.z0, ownBox.x1, ownBox.z1, containment.size());
        }
    }

    private final Node root;
    private final BaseMesh mesh;

    public QTree(BaseMesh mesh, int CONTAINMENT_THRESHOLD, int MAXIMAL_DEPTH) {
        this.CONTAINMENT_THRESHOLD = CONTAINMENT_THRESHOLD;
        this.MAXIMAL_DEPTH = MAXIMAL_DEPTH;
        this.mesh = mesh;
        root = new Node(mesh.boxXZ);
        mesh.trianglesXZ.forEach(root::assign);
    }

    public QTree(BaseMesh mesh) {
        this(mesh, 10, 10); //TODO research this constants in browser. Depends on fixing QTree redundancy
    }

    public double query(Coordinate point) {
        ArrayList<Polygon> list = root.query(point.x, point.y);
        assert list.size() != 0 : "Queried non-covered with mesh point, or an error occurred in QTree: queried list is empty";
        Point geometryPoint = gf.createPoint(point);
        Polygon response = null;
        for (Polygon triangle : list) {
            if (triangle.covers(geometryPoint)) {
                response = triangle;
                break;
            }
        }
        if (response == null) throw new RuntimeException("An error seems to be occurred in QTree");
        Mesh.Triplet t = mesh.faceIndices.get((Integer) response.getUserData());
        double A = mesh.vertexesY.get(t.a), B = mesh.vertexesY.get(t.b), C = mesh.vertexesY.get(t.c);
        return BarycentricCoordinate.fromCartesian2D(point, response).getWeightedHeightIn3D(A, B, C);
    }

    @Override
    public String toString() {
        System.out.println("generating string view...");
        return "Tree view, note CONT_THR is " + CONTAINMENT_THRESHOLD + " and MAX_DPTH is " + MAXIMAL_DEPTH + "\n" +
                "main box is: " + root.ownBox.toString() + '\n' +
                "cnt is: " + root.cnt() + '\n' +
//                root.toString() + '\n' +
                "";
    }

    public Box getXZBox(){
        return root.ownBox;
    }

    void dumpToJS(BufferedWriter bw) throws IOException{
//        bw.write("rootbb: " + root.ownBox.toString() + "\n");
        bw.write("\"tree\":\n");
        root.dfd(bw);
        CommandLineUtils.reportFinish();
    }

    void dumpToBoxes(PrintWriter pw){
        root.dumpToBoxes(pw);
    }
}
