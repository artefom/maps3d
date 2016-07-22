package Utils;

/**
 * Created by Artem on 20.07.2016.
 */
public class Pair<C1,C2> {

    C1 v1;
    C2 v2;
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
