package Algorithm.NearbyGraph;

import Isolines.IIsoline;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class Isoline_attributed {

    public static class LineSide {

        private Isoline_attributed isoline;

        private LineSide other;
        private boolean positive;

        private LineSide(Isoline_attributed isoline, boolean positive) {
            this.isoline = isoline;
            this.positive = positive;
        }

        public LineSide getOther() {
            return other;
        }

        public boolean isPositive() {
            return positive;
        }

        public boolean isNegative() {
            return !positive;
        }

        public int getSign(){ return this.positive ? 1 : -1; }

        public Isoline_attributed getIsoline() {
            return isoline;
        }

        @Override
        public String toString() {
            return "(LS"+ (positive ? "+: " : "-: ") +isoline.toString()+")";
        }
    }

    private IIsoline isoline;
    private LineSide ls_positive;
    private LineSide ls_negative;
    public boolean height_recovered = false;

    //public HashMap<Isoline_attributed, Integer > outcomming;

    private Isoline_attributed(IIsoline isoline) {
        this.isoline = isoline;
        ls_positive = new LineSide(this,true);
        ls_negative = new LineSide(this,false);
        ls_negative.other = ls_positive;
        ls_positive.other = ls_negative;
    }


    public static Isoline_attributed fromIsoline(IIsoline isoline) {
        return new Isoline_attributed(isoline);
    }

    public IIsoline getIsoline() {
        return isoline;
    }

    public LineSide getSidePositive() {
        return ls_positive;
    }

    public LineSide getSideNegative() {
        return ls_negative;
    }

    /**
     * Get Line side by it's index (-1 for negative side, 1 for positive side)
     * if id != -1 and id != 1 throws Runtime exception
     */
    public LineSide getSideByIndex(int id) {
        if (id == -1) return getSideNegative();
        if (id == 1) return getSidePositive();
        throw new RuntimeException("Unknown side index");
    }

    @Override
    public String toString() {
        return this.isoline.toString();
    }

}
