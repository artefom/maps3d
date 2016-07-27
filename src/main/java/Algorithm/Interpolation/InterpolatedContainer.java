package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.Isoline;
import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class InterpolatedContainer {

    GeometryFactory gf;
    Isoline_attributed[] isolines;

    public InterpolatedContainer(IsolineContainer container) {
        this.isolines = new Isoline_attributed[container.size()];
        int i = 0;
        for (IIsoline iso : container) {
            isolines[i] = new Isoline_attributed(iso);
            i+=1;
        }
    }

    public void match(Isoline_attributed iso) {
        for (int i = 0; i != isolines.length; ++i) {
            Isoline_attributed target = isolines[i];
            if (target.getIsoline() != iso.getIsoline()) {
                iso.matchIfLess(target);
            }
        }
    }

}
