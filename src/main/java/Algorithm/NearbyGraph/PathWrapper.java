package Algorithm.NearbyGraph;

import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

///**
// * Created by Artyom.Fomenko on 26.07.2016.
// */
//public class PathWrapper {
//
//    ArrayList<Isoline_attributed.LineSide> nodes;
//
//    private PathWrapper(ArrayList<Isoline_attributed.LineSide> nodes) {
//        this.nodes = nodes;
//    }
//
//    /**
//     * Creates path wrapper from collection, where first element of collection - first path node
//     * @param path
//     * @return
//     */
//    public static PathWrapper fromPath(Collection<Isoline_attributed.LineSide> path) {
//        ArrayList<Isoline_attributed.LineSide> nodes = new ArrayList<>(path.size());
//        path.forEach(nodes::add);
//        return new PathWrapper(nodes);
//    }
//
//    /**
//     * Creates path wrapper from collection, where last element of collection - first path node
//     * @param path
//     * @return
//     */
//    public static PathWrapper fromPathReversed(Collection<Isoline_attributed.LineSide> path) {
//        ArrayList<Isoline_attributed.LineSide> nodes = new ArrayList<>(path.size());
//        for (int i = 0; i != path.size(); ++i) nodes.add(null);
//        Iterator<Isoline_attributed.LineSide> it = path.iterator();
//        for (int i = path.size()-1; i >= 0; --i) {
//            nodes.set(i,it.next());
//        }
//        return new PathWrapper(nodes);
//    }
//
//    public Isoline_attributed getTargetIsoline() {
//        return nodes.get(nodes.size()-1).getIsoline();
//    }
//
//    public Isoline_attributed getSourceIsoline() {
//        return nodes.get(0).getIsoline();
//    }
//
//    public boolean reversed() {
//        boolean result = false;
//        Iterator<Isoline_attributed.LineSide> it = nodes.iterator();
//        Isoline_attributed.LineSide prev = it.next();
//        while (it.hasNext()) {
//            Isoline_attributed.LineSide next = it.next();
//            if (prev.isPositive() == next.isPositive()) result = !result;
//            prev = next;
//        }
//        return result;
//    }
//
//}
