package ru.ogpscenter.maps3d.isolines;

import com.vividsolutions.jts.geom.*;
import ru.ogpscenter.maps3d.utils.DebugUtils;

/**
 * Created by Artyom.Fomenko on 15.07.2016.
 */
public class Isoline implements IIsoline {

    private final LineString lineString;
    private int type;
    private int slope_side;
    private int id;
    private boolean isedgetoedge;
    private double height;

    public Isoline(int type, int side, CoordinateSequence cs, GeometryFactory gf) {
        lineString = new LineString(cs,gf);
        id = ++DebugUtils.isoline_last_id;
        slope_side = side;
        this.type = type;
        isedgetoedge = false;
        height = 0;
    }

    public Isoline(IIsoline other) {
        lineString = new LineString(other.getLineString().getCoordinateSequence(),
                other.getLineString().getFactory());
        id = ++DebugUtils.isoline_last_id;
        slope_side = other.getSlopeSide();
        type = other.getType();
        isedgetoedge = other.isEdgeToEdge();
        height = other.getHeight();
    }

    public int getType() {
        return  type;
    }

    @Override
    public boolean isClosed() {
        return this.lineString.isClosed();
    }

    public void setEdgeToEdge(boolean isedgetoedge) {
        this.isedgetoedge = isedgetoedge;
    }

    @Override
    public boolean isEdgeToEdge() {
        return isedgetoedge;
    }

    @Override
    public int getID() {
        return Isoline.this.id;
    }

    @Override
    public void setID(int id) {this.id = id;};

    @Override
    public Geometry getGeometry() {
        return lineString;
    }

    @Override
    public LineString getLineString() {
        return lineString;
    }

    @Override
    public GeometryFactory getFactory() {
        return lineString.getFactory();
    }

//    public static IIsoline asRing(IIsoline il) {
//        return new Isoline(il.getType(),il.getSlopeSide(),
//                getLoopedCoordinates(il),il.getGeometry().getFactory());
//    }

    @Override
    public int getSlopeSide() {
        return slope_side;
    }

    @Override
    public void setSlopeSide(int side) {
        slope_side = side;
    }


    @Override
    public int hashCode() {
        Coordinate p1 = lineString.getCoordinateN(0);
        Coordinate p2 = lineString.getCoordinateN(lineString.getNumPoints() - 1);
        if (slope_side != 0) {
            if (slope_side == 1) {
                return ((31 * Double.hashCode(p2.x) - 51 * Double.hashCode(p2.y))
                        -(3 * Double.hashCode(p1.x) - 17 * Double.hashCode(p1.y))) + 129 * type;
            } else {
                return ((31 * Double.hashCode(p1.x) - 51 * Double.hashCode(p1.y))
                        -(3 * Double.hashCode(p2.x) - 17 * Double.hashCode(p2.y))) + 129 * type;
            }
        } else {
            return ((31 * Double.hashCode(p1.x) - 51 * Double.hashCode(p1.y))
                   +(31 * Double.hashCode(p2.x) - 51 * Double.hashCode(p2.y))) + 129 * type;
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Isoline))return false;
        Isoline other = (Isoline)obj;
        return equals(other);
    }

    public boolean equals(Isoline other) {
        if (lineString.getNumPoints() != other.lineString.getNumPoints()) return false;
        if (Isoline.this.type != other.type) return false;

        boolean exact_match = true;
        boolean reversed_match = true;

        for (int i = 0; i != lineString.getNumPoints(); ++i) {
            if ( !lineString.getCoordinateN(i).equals( other.lineString.getCoordinateN(i) ) ) {
                exact_match = false;
                break;
            }
        }

        for (int i = 0; i != lineString.getNumPoints(); ++i) {
            if (!lineString.getCoordinateN(i).equals(other.lineString.getCoordinateN(lineString.getNumPoints()-1-i))) {
                reversed_match = false;
                break;
            }
        }

        if (exact_match) {
            if (Isoline.this.slope_side == other.slope_side) return true;
        }
        if (reversed_match) {
            if (Isoline.this.slope_side == -other.slope_side) return true;
        }

        //if (this.slope_side != other.slope_side) return false;
        return false;
    }

    @Override
    public String toString() {
        LineString ls = getLineString();
        int end = getLineString().getNumPoints()-1;
        double startx = ls.getCoordinateN(0).x;
        double starty = ls.getCoordinateN(0).y;
        double endx = ls.getCoordinateN(end).x;
        double endy = ls.getCoordinateN(end).y;
        return "ISOLINE_"+getSlopeSide()+"_"+getType()+"_h="+getHeight()+"(" + startx + "," + starty + " - " + endx + "," + endy + ")";
    }

    @Override
    public boolean isSteep() {
        return type == 4;
    }

    @Override
    public boolean isHalf() {
        return type == 1;
    }

    @Override
    public double getHeight() {
        return this.height;
    }

    @Override
    public void setHeight(double height) {
        this.height = height;
    }
}
