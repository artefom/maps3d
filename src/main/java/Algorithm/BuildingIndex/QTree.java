package Algorithm.BuildingIndex;

import toxi.geom.*;

import java.util.ArrayList;

/**
 * Created by fdl on 8/4/16.
 */
public class QTree {
    private final int CONTAINMENT_THRESHOLD;// = 4;//TODO research this const
    private final int MAXIMAL_DEPTH;// = 30;//239; //TODO and this one too

    public class Node {
        private final Node parent;
        private final int depth;
        private final Box ownBox;

        private boolean isSplit = false;
        private final Node[] quarters = new Node[4];
        private final ArrayList<WrappedTriangle> containment = new ArrayList<>();

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

        protected void assign(WrappedTriangle wTriangle) {
            if (isSplit) {
                for (Node node : quarters) {
                    if (node.ownBox.intersects(wTriangle.triFlat)) {
                        node.assign(wTriangle);
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
            containment.add(wTriangle);
            if (depth < MAXIMAL_DEPTH && containment.size() >= CONTAINMENT_THRESHOLD) { //TODO somewhere over-density should be checked. Is max depth ok?
                split();
                containment.forEach(this::assign);
                containment.clear();
            }
//            if (depth == MAXIMAL_DEPTH) System.err.println("got maximal depth"); //FIXME
        }

        protected ArrayList<WrappedTriangle> query(Vec3D p) {
            assert p.getComponent(1) == 0f : "query point should lay in XZ surface"; //TODO test and remove
            if (isSplit) {
                for (Node node : quarters) {
                    if (node.ownBox.containsPoint(p)) {
                        return node.query(p);
                    }
                }
            } else {
                return containment;
            }
            return null;
        }

        @Override
        public String toString(){
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
                sb.append("represents: ").append(FlatUtils.toString(ownBox)).append('\n');
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

        protected int cnt(){
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
    }

    private final Node root;

    public QTree(Box coverageArea, int CONTAINMENT_THRESHOLD, int MAXIMAL_DEPTH) {
        this.CONTAINMENT_THRESHOLD = CONTAINMENT_THRESHOLD;
        this.MAXIMAL_DEPTH = MAXIMAL_DEPTH;
        root = new Node(coverageArea);
    }

    public QTree(Box coverageArea) {
        this(coverageArea, 10, 10);
    }

    public void add(WrappedTriangle wTri){
        root.assign(wTri);
    }

    public ArrayList<WrappedTriangle> query(Vec3D p) {
        return root.query(p);
    }

    @Override
    public String toString(){
        System.out.println("generating string view...");
        return "Tree view, note CONT_THR is " + CONTAINMENT_THRESHOLD + " and MAX_DPTH is " + MAXIMAL_DEPTH + "\n" +
                "main box is: " + root.ownBox.toString() + '\n' +
                "cnt is: " + root.cnt() + '\n' +
//                root.toString() + '\n' +
                "";
    }
}
