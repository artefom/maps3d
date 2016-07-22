package Isolines;

import com.vividsolutions.jts.geom.*;

/**
 * Created by Artem on 16.07.2016.
 */
public interface IIsoline {

    int getSlopeSide();
    int getType();

    //Debug use Only;
    static int last_id = 0;
    int getID();

    Geometry getGeometry();
    LineString getLineString();

    GeometryFactory getFactory();

}
