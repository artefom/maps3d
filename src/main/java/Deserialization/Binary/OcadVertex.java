package Deserialization.Binary;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artyom.Fomenko on 19.08.2016.
 */
public class OcadVertex extends Coordinate {

    boolean IS_BEIZER;
    boolean EMPTY_LEFT;
    boolean AREA_BORDER;
    boolean CONNER;
    boolean HOLE_FIRST;
    boolean EMPTY_RIGHT;
    boolean IS_DASH;

    OcadVertex() {

    }

    public boolean isBezier(){return IS_BEIZER;};
    public boolean isEmptyLeft() {return EMPTY_LEFT;};
    public boolean isAreaBorder(){return AREA_BORDER;};
    public boolean isConner(){return CONNER;};
    public boolean isHoleFirst(){return HOLE_FIRST;};
    public boolean isEmptyRight(){return EMPTY_RIGHT;};
    public boolean isDash(){return IS_DASH;};

}
