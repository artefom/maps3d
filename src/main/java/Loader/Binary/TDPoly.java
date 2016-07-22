package Loader.Binary;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created by Artem on 21.07.2016.
 */
public class TDPoly {
    @byteoffset( offset = 0)
    public int x;

    @byteoffset( offset = 4)
    public int y;

    // For internal use for parsing values from file
    static int INTERNAL_BEZIER_FIRST	= (1 << 0);
    static int INTERNAL_BEZIER_SECOND	= (1 << 1);
    static int INTERNAL_EMPTY_LEFT 	= (1 << 2);
    static int INTERNAL_AREA_BORDER	= (1 << 3);

    static int INTERNAL_CONNER		    = (1 << 0);
    static int INTERNAL_HOLE_FIRST	    = (1 << 1);
    static int INTERNAL_EMPTY_RIGHT    = (1 << 2);
    static int INTERNAL_IS_DASH	 	= (1 << 3);

    // Flags for public use. Use them in coupe with getFlags()
    // As may be seen, yFlags are upper bits and xFlags are lower.
    public static int BEZIER_FIRST	    = (1 << 0);
    public static int BEZIER_SECOND	= (1 << 1);
    public static int EMPTY_LEFT 		= (1 << 2);
    public static int AREA_BORDER	 	= (1 << 3);
    public static int CONNER			= (1 << 4);
    public static int HOLE_FIRST		= (1 << 5);
    public static int EMPTY_RIGHT 	    = (1 << 6);
    public static int IS_DASH	 		= (1 << 7);

    public int getFlags() {
        return (x&((1 << 4)-1)) | ((y&((1 << 4)-1))<<4);
    };

    public int getX() {
        return x >> 4;
    }

    public int getY() {
        return y >> 4;
    }

    public boolean isNull() {
        return x == 0 && y == 0;
    }

    public Coordinate toCoordinate() {
        return new Coordinate((double)getX()/1600,(double)getY()/1600);
    }

    public boolean isBezier() {
        if ( (getFlags() & (BEZIER_FIRST | BEZIER_SECOND)) != 0) return true;
        return false;
    }

    public boolean isConner() {
        if ( (getFlags() & (CONNER)) != 0) return true;
        return false;
    }

    public boolean isDash() {
        if ( (getFlags() & (IS_DASH)) != 0) return true;
        return false;
    }


}
