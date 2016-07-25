package Algorithm.NearbyGraph;

import Isolines.IIsoline;
import Isolines.IsolineContainer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class GraphBuilder {

    ArrayList<Isoline_attributed> iso_nodes;

    NearbyEstimator nearbyEstimator;

    private int current_mark;

    public GraphBuilder(IsolineContainer container) {
        current_mark = 0;
        nearbyEstimator = new NearbyEstimator(container.getFactory());
        iso_nodes = new ArrayList<>();
        for (IIsoline iso : container) {
            iso_nodes.add( new Isoline_attributed(iso));
        }
    }

    public void buildConnections() {
        nearbyEstimator.estimate(iso_nodes);
        BuildMinimumSpanningTree();
    }

    /**
     * Checks, whether the graph has one connected component
     * @return
     */
    public boolean isOneConnectedComponent() {
        Deque<Isoline_attributed> dfs_queue = new ArrayDeque<>();
        dfs_queue.add(iso_nodes.get(0));
        int visited_count = 1;
        while (dfs_queue.size() > 0) {
            Isoline_attributed iso = dfs_queue.pollFirst();
            visit(iso);
            for (NearbyConnection con : iso.outcomming) {
                if (con.isEnabled() && !isVisited(con.to)) {
                    dfs_queue.addLast(con.to);
                    visited_count += 1;
                }
            }
            for (NearbyConnection con : iso.incomming) {
                if (con.isEnabled() && !isVisited(con.from)) {
                    dfs_queue.addLast(con.from);
                    visited_count += 1;
                }
            }
        }
        return visited_count == iso_nodes.size();
    }

    public ArrayList<NearbyConnection> getSortedConnections() {
        ArrayList<NearbyConnection> ret = new ArrayList<>();
        for (Isoline_attributed iso : iso_nodes) {
            for (NearbyConnection con : iso.outcomming) {
                ret.add(con);
            }
        }
        ret.sort((lhs,rhs)-> Integer.compare(lhs.weight,rhs.weight));
        return ret;
    }

    public void BuildMinimumSpanningTree() {
        ArrayList<NearbyConnection> connections = getSortedConnections();
        for (NearbyConnection con : connections) {
            con.disable();
            if (isOneConnectedComponent()) {
                con.destroy();
            } else {
                con.enable();
            }
        }
    };

    public boolean isVisited(Isoline_attributed iso) {
        return iso.getMark() == current_mark;
    }

    public void visit(Isoline_attributed iso) {
        iso.setMark(current_mark);
    }

    public void resetVisits() {
        current_mark += 1;
    }

}
