package Display;

import Algorithm.EdgeDetection.Edge;
import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.*;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Artem on 17.07.2016.
 * Class translates isoline container to drawing primitives, which are then processed
 * by renderer
 */
public class Drawer {

    GeometryFactory gf;
    public Drawer( GeometryFactory gf ) {
        this.gf = gf;
    }

    /**
     * Convers IIsoline Container to geometry
     * @param isolines
     * @return
     */
    public List<GeometryWrapper> draw(IsolineContainer isolines) {
        ArrayList<GeometryWrapper> geom = new ArrayList<>();

        for (IIsoline i: isolines) {

            LineString ls = i.getLineString();

            if (Constants.DRAWING_INTERPOLATION)
                ls = interpolatedLine(ls,Constants.DRAWING_INTERPOLATION_STEP);
            Color col = i.getLineString().isClosed() ?
                    Constants.DRAWING_COLOR_ISOLINE_CLOSED:
                    Constants.DRAWING_COLOR_ISOLINE;

            geom.add( new GeometryWrapper( ls, col, i.getType()*Constants.DRAWING_LINE_WIDTH ));
            double d = (double)i.getType()*Constants.DRAWING_POINT_WIDTH;
            geom.add( new GeometryWrapper( gf.createPoint(ls.getCoordinateN(0)), col, d ));
        }
        return geom;
    }

    private LineString interpolatedLine(LineString ls, double step) {
        LineStringInterpolatedPointIterator it = new LineStringInterpolatedPointIterator(ls,step);

        LinkedList<Coordinate> coords_list = new LinkedList<>();
        int i = 0;
        while (it.hasNext()) {
            Coordinate c = it.next();
            coords_list.add(c);
            i += 1;
        }
        return gf.createLineString(coords_list.toArray(new Coordinate[coords_list.size()]));
    }

    public GeometryWrapper draw(Edge edge) {
        LineString ls = edge.outerBound;
        return new GeometryWrapper(ls,Color.RED,1);
    }
}
