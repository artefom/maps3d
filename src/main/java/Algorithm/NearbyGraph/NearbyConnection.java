package Algorithm.NearbyGraph;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class NearbyConnection {
    public Isoline_attributed to;
    public Isoline_attributed from;
    public int from_side;
    public int to_side;
    public int weight;
    private boolean disabled;

    public NearbyConnection() {

        to = null;
        from = null;
        disabled = true;
        from_side = 0;
        to_side = 0;
    }

    public NearbyConnection(Isoline_attributed from, Isoline_attributed to, int from_side, int to_side, int weight) {
        this.from = from;
        this.to = to;
        this.from_side = from_side;
        this.to_side = to_side;
        this.weight = weight;
        disabled = false;
    }

    public void disable() {
        disabled = true;
    }

    public void enable() {
        disabled = false;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isEnabled() {
        return !disabled;
    }

    public void destroy() {
        boolean ok = from.destroyConnection(this);
        if (!ok) throw new RuntimeException("NearbyConnection reffers to node as 'from', but node does not know this connection");
    }

    public boolean same(NearbyConnection other) {
        if (other.from_side == this.from_side && other.to_side == this.to_side && other.from == this.from && this.to == other.to) return true;
        if (other.from_side == this.to_side && other.to_side == this.from_side && other.from == this.to && other.to == this.from) return true;
        return false;
    }

    public boolean inverted(NearbyConnection other) {
        if (other.to_side == this.from_side && other.from_side == this.to_side && other.from == this.from && this.to == other.to) return true;
        if (other.to_side == this.to_side && other.from_side == this.from_side && other.from == this.to && other.to == this.from) return true;
        return false;
    }


}
