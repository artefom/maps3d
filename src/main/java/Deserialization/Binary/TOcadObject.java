package Deserialization.Binary;

import Utils.Curves.CurveString;
import Utils.Constants;
import Utils.Pair;
import com.sun.org.apache.xpath.internal.functions.Function2Args;
import com.vividsolutions.jts.geom.*;

import javax.security.auth.callback.TextOutputCallback;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;

class TDPoly {
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

//    public double getX() {
//        return realX;
//        //return (x >> 4) / Constants.map_scale_fix;
//    }
//
//    public double getY() {
//        return realY;
//        //return (y >> 4) / Constants.map_scale_fix;
//    }
//
//    public void

    public int getXOriginal() {
        return x >> 4;
    }

    public int getYOriginal() {
        return y >> 4;
    }

    public boolean isNull() {
        return x == 0 && y == 0;
    }

    public boolean isBezier() {
        return (getFlags() & (BEZIER_FIRST | BEZIER_SECOND)) != 0;
    }

    public boolean isConner() {
        return (getFlags() & (CONNER)) != 0;
    }

    public boolean isDash() {
        return (getFlags() & (IS_DASH)) != 0;
    }

    public boolean isEmptyLeft() {
        return (getFlags() & (EMPTY_LEFT)) != 0;
    }

    public boolean isEmptyRight() {
        return (getFlags() & (EMPTY_RIGHT)) != 0;
    }

    public boolean isAreaBorder() {
        return (getFlags() & (AREA_BORDER)) != 0;
    }

    public boolean isHoleFirst() {
        return (getFlags() & (HOLE_FIRST)) != 0;
    }

    @Override
    public String toString() {
        String flags = "";
        if (isNull()) flags += "Null ";
        if (isBezier()) flags += "Bezier ";
        if (isConner()) flags += "Conner ";
        if (isDash()) flags += "Dash ";
        if (isEmptyLeft()) flags += "EmptyLeft ";
        if (isEmptyRight()) flags += "EmptyRight ";
        if (isAreaBorder()) flags += "AreaBorder ";
        if (isHoleFirst()) flags += "HoleFirst ";
        return "TDPoly( "+getXOriginal()+";"+getYOriginal()+"; " + flags + ")";
    }
}


/**
 * Created by Artem on 21.07.2016.
 */
public class TOcadObject extends ByteDeserializable {

    private final int TDPoly_offset = 32;
    private final int TDPoly_size = 8;
    private final ArrayList<TDPoly> Poly = new ArrayList<>();

    public final ArrayList<OcadVertex> vertices = new ArrayList<>();

    private Function< Pair<Integer,Integer>, Coordinate> coordinateConverter;
    public TOcadObject(Function< Pair<Integer,Integer>, Coordinate> coordinateConverter ) {
        this.coordinateConverter = coordinateConverter;
    }

    @Override
    public void Deserialize(SeekableByteChannel s, int offset, ByteBuffer buf) {
        super.Deserialize(s, offset, buf);
        Object[] obj;
        try {
            obj = readObjectArray(offset+TDPoly_offset, this.nItem+1, TDPoly_size, TDPoly.class);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid format");
        }

        Poly.clear();
        for (int i = 0; i != obj.length; ++i) {
            TDPoly poly = (TDPoly)obj[i];
            if (!poly.isNull()) {
                this.Poly.add(poly);

                Coordinate newCoordinates = coordinateConverter.apply(new Pair<>(poly.getXOriginal(),poly.getYOriginal()));
                OcadVertex vertex = new OcadVertex();

                vertex.x = newCoordinates.x;
                vertex.y = newCoordinates.y;
                vertex.IS_BEIZER = poly.isBezier();
                vertex.EMPTY_LEFT = poly.isEmptyLeft();
                vertex.AREA_BORDER = poly.isAreaBorder();
                vertex.CONNER = poly.isConner();
                vertex.HOLE_FIRST = poly.isHoleFirst();
                vertex.EMPTY_RIGHT = poly.isEmptyRight();
                vertex.IS_DASH = poly.isDash();

                this.vertices.add( vertex );
            }
        }
        this.nItem = this.Poly.size();
    }

    public boolean isLine() {
//        return Sym == 101000 ||
//                Sym == 102000 ||
//                Sym == 103000 ||
//                Sym == 106001 ||
//                Sym == 106000 ||
//                Sym == 106006;

        return Sym == 101001 ||
                Sym == 102002 ||
                Sym == 103001 ||
                Sym == 106004 ||
                Sym == 106006 ||
                Sym == 106005;
    }

    public boolean isSlope() {
        return (Sym == 104004 || Sym == 104003);
    }

    public int getType() {
//        if (Sym == 101000) return 2;
//        if (Sym == 102000) return 3;
//        if (Sym == 103000) return 1;
//        if (Sym == 106001 || Sym == 106000 || Sym == 106006) return 4;
//        return -1;
        if (Sym == 101001) return 2;
        if (Sym == 102002) return 3;
        if (Sym == 103001) return 1;
        if (Sym == 106004 || Sym == 106005 || Sym == 106006) return 4;
        return -1;
    }



    public Geometry getGeometry(GeometryFactory gf) throws Exception {

        // Split into circles, if succeeded, means that it's polygon

        if (Otp == 3) {// Area object
            ArrayList<LinearRing> rings = new ArrayList<>();
            ArrayList<TDPoly> poly = new ArrayList<>();
            for (TDPoly p : Poly) {
                if (!p.isHoleFirst()) {
                    poly.add(p);
                } else {
                    if (poly.size() >= 4) {
                        poly.add(poly.get(0));
                        try {
                            LinearRing r = gf.createLinearRing(CurveString.fromOcadVertices(vertices).interpolate(Constants.DESERIALIZATION_BEZIER_STEP, gf).getCoordinateSequence());
                            rings.add(r);
                        } catch (Exception ex) {
                            System.out.println("ERROR: "+ex.getMessage());
                            throw new Exception(ex.getMessage());
                        }
                    }
                    poly.clear();
                    poly.add(p);
                }
            }
            if (poly.size() >= 4) {
                poly.add(poly.get(0));
                try {
                    LinearRing r = gf.createLinearRing(CurveString.fromOcadVertices(vertices).interpolate(Constants.DESERIALIZATION_BEZIER_STEP, gf).getCoordinateSequence());
                    rings.add(r);
                } catch (Exception ex) {
                    System.out.println("ERROR: "+ex.getMessage());
                    throw new Exception(ex.getMessage());
                }
            }
            // Rings gathered. Now transform them into polygon
            if (rings.size() > 1) {
                return gf.createPolygon(rings.get(0),rings.subList(1,rings.size()).toArray(new LinearRing[rings.size()-1]));
            } else if (rings.size() == 1) {
                return gf.createPolygon(rings.get(0));
            };
            return null;
        } else if (Otp == 2) { // Line Object
            CurveString cs = CurveString.fromOcadVertices(vertices);
            return cs.interpolate(Constants.DRAWING_INTERPOLATION_STEP,gf);
        } else {
            return null;
        }
    }

    @byteoffset( offset = 0)
    public int Sym;

    @byteoffset( offset = 4)
    public byte Otp;

    @byteoffset( offset = 5)
    public byte _Customer;

    @byteoffset( offset = 6)
    public short Ang;

    @byteoffset( offset = 8)
    public int nItem;

    @byteoffset( offset = 12)
    public short nText;

    @byteoffset( offset = 14)
    public byte Mark;

    @byteoffset( offset = 15)
    public byte SnappingMark;

    @byteoffset( offset = 16)
    public int Col;

    @byteoffset( offset = 20)
    public short LineWidth;

    @byteoffset( offset = 22)
    public short DiamFlags;

    @byteoffset( offset = 24)
    public int ServerObjectId;

    @byteoffset( offset = 28)
    public int Height;
}
