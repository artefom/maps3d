package mouse;

import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by Artem on 21.08.2016.
 */
public class MouseActionController {

    private ActionBase action;

    private ArrayDeque<Coordinate> coordinates_buffer;

    public MouseActionController() {

        action = null;
        coordinates_buffer = new ArrayDeque<>();
    }

    public final void addPoint(Coordinate c) {

        coordinates_buffer.add(c);

    }

    public void clear() {
        coordinates_buffer.clear();
    }

    public void execute(IsolineContainer cont, double near_threshold) throws Exception {
        if (action == null) throw new Exception("No tool");
        action.execute(coordinates_buffer.toArray(new Coordinate[coordinates_buffer.size()]),cont, near_threshold);
    }

    public void setAction(ActionBase action) {
        this.action = action;
    }

    public ActionBase getAction() {
        return action;
    }

    public Deque<Coordinate> getCoordinates() {
        return coordinates_buffer;
    }

}
