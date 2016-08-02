package Utils;

/**
 * Pair of values. When will java have it's own?
 */
public class Pair<C1,C2> {

    public C1 v1;
    public C2 v2;
    public Pair(C1 v1, C2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public C1 getKey() {
        return v1;
    }

    public C2 getValue() {
        return v2;
    }

    public C1 first() {
        return v1;
    }

    public C2 second() {
        return v2;
    }

}
