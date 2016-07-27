package Algorithm.NearbyGraph;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artyom.Fomenko on 26.07.2016.
 */
public class NearbyContainer {

    private ArrayList<Isoline_attributed> isolines;

    public NearbyContainer(Collection<IIsoline> isolines) {
        this.isolines = new ArrayList<>();
        for (IIsoline iso : isolines) {
            this.isolines.add( Isoline_attributed.fromIsoline(iso) );
        }
    }

    public Set<Isoline_attributed.LineSide> getSideSet() {
        Set<Isoline_attributed.LineSide> ret = new HashSet<>();
        for (Isoline_attributed iso : isolines) {
            ret.add(iso.getSideNegative());
            ret.add(iso.getSidePositive());
        }
        return ret;
    }

    public ArrayList<Isoline_attributed> getIsolines() {
        return isolines;
    }
}
