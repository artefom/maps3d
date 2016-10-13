package ru.ogpscenter.maps3d.isolines;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Created by Artem on 16.07.2016.
 */
public interface IIsoline {

    int getSlopeSide();
    void setSlopeSide(int side);
    int getType();

    double getHeight();
    void setHeight(double height);

    boolean isClosed();
    boolean isSteep();
    boolean isHalf();

    void setEdgeToEdge(boolean isedgetoedge);
    boolean isEdgeToEdge();

    //Debug use Only;
    static int last_id = 0;
    int getID();
    void setID(int id);

    Geometry getGeometry();
    LineString getLineString();

    GeometryFactory getFactory();

}
