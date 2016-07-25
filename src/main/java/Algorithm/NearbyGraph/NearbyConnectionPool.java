package Algorithm.NearbyGraph;

import java.util.ArrayList;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class NearbyConnectionPool {

    ArrayList<NearbyConnection> pool;

    public NearbyConnectionPool() {
        pool = new ArrayList<>();
    }

    /**
     * TODO: optimize same/inverted search with hash functions
     * @param connection
     */
    public void add(NearbyConnection connection) {
        for (NearbyConnection con : pool) {
            if (con.same(connection)) {
                con.weight += connection.weight;
                return;
            }
            if (con.inverted(connection)) {
                con.weight -= connection.weight;
                return;
            }
        }
        pool.add(connection);
    }


}
