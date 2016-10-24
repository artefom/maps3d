package Deserialization.Binary;

import com.vividsolutions.jts.geom.*;
import ru.ogpscenter.maps3d.utils.Pair;
import ru.ogpscenter.maps3d.utils.curves.CurveString;
import ru.ogpscenter.maps3d.utils.properties.PropertiesLoader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Function;

class TDPoly {
    @ByteOffset( offset = 0)
    public int x;

    @ByteOffset( offset = 4)
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

    private static final int TDPOLY_OFFSET = 32;
    private static final int TDPOLY_SIZE = 8;
    private final ArrayList<TDPoly> Poly = new ArrayList<>();

    public final ArrayList<OcadVertex> vertices = new ArrayList<>();

    private Function< Pair<Integer,Integer>, Coordinate> coordinateConverter;
    public TOcadObject(Function< Pair<Integer,Integer>, Coordinate> coordinateConverter ) {
        this.coordinateConverter = coordinateConverter;
    }

    @Override
    public void deserialize(ByteBuffer s, final int offset) {
        super.deserialize(s, offset);
        Object[] obj;
        try {
            obj = readObjectArray(offset + TDPOLY_OFFSET, this.nItem + 1, TDPOLY_SIZE, TDPoly.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if (message == null) {
                message =  ex.getClass().getName();
            }
            throw new RuntimeException("Invalid format: " + message);
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
//        System.out.println(offset + " " + this.toString());
    }

    int isLineCache = 0;
    public boolean isLine() {
        return getType() != -1;
    }

    private int isSlope_cache = 0;
    public boolean isSlope() {
        if (isSlope_cache == 0) {
            isSlope_cache = PropertiesLoader.ocad_input.isSlope(Sym) ? 1 : -1;
        }
        return isSlope_cache == 1;
    }

    private int line_type_cache = -2;
    public int getType() {
        if (line_type_cache == -2) {
            line_type_cache = PropertiesLoader.ocad_input.getLineType(Sym);
        }
        return line_type_cache;
    }

    private static ArrayList< ArrayList<OcadVertex> > splitByHoleFirst(ArrayList<OcadVertex> vertices) {
        ArrayList< ArrayList<OcadVertex> > ret = new ArrayList<>();

        ArrayList<OcadVertex> buf = new ArrayList<>();

        for (OcadVertex v : vertices) {
            if (v.HOLE_FIRST) { // Hole first encountered, enclose linestring 'buf' and put it into ret.
                if (buf.size() >= 3) {
                    buf.add(buf.get(0));
                    ret.add(buf);
                }
                buf = new ArrayList<>();
            }
            buf.add(v);
        }
        // Enclose buf residue and add it into ret.\
        if (buf.size() >= 3) {
            buf.add(buf.get(0));
            ret.add(buf);
        }
        return ret;
    }


    public Geometry getGeometry(GeometryFactory gf) throws Exception {

        // Split into circles, if succeeded, means that it'buffer polygon

        if (Otp == 3) {// Area object

            ArrayList<TDPoly> poly = new ArrayList<>();

            // Run through vertecies and add them to buffer, while hole first not encountered...

            // Exterior ring - first element of this array, other are holes
            ArrayList< ArrayList<OcadVertex> > exteriorAndHoles = splitByHoleFirst(vertices);

            if (exteriorAndHoles.size() == 0) {
                throw new RuntimeException("Ocad object has no geometry");
            }

            ArrayList<OcadVertex> exterior = exteriorAndHoles.get(0);
            ArrayList< ArrayList<OcadVertex> > holes = new ArrayList<>();
            for (int i = 1; i < exteriorAndHoles.size(); ++i) {
                holes.add(exteriorAndHoles.get(i));
            }

            LineString exterior_string = CurveString.fromOcadVertices(exterior).interpolate(gf);
            LinearRing exterior_ring = gf.createLinearRing(exterior_string.getCoordinateSequence());
            ArrayList<LinearRing> interior_rings = new ArrayList<>();

            // Rings gathered. Now transform them into polygon

            return gf.createPolygon(exterior_ring,interior_rings.toArray(new LinearRing[interior_rings.size()]));

        } else if (Otp == 2) { // Line Object
            CurveString cs = CurveString.fromOcadVertices(vertices);
            return cs.interpolate(gf);
        } else {
            return null;
        }
    }

    @ByteOffset( offset = 0)
    public int Sym;

    @ByteOffset( offset = 4)
    public byte Otp;

    @ByteOffset( offset = 5)
    public byte _Customer;

    @ByteOffset( offset = 6)
    public short Ang;

    @ByteOffset( offset = 8)
    public int nItem;

    @ByteOffset( offset = 12)
    public short nText;

    @ByteOffset( offset = 14)
    public byte Mark;

    @ByteOffset( offset = 15)
    public byte SnappingMark;

    @ByteOffset( offset = 16)
    public int Col;

    @ByteOffset( offset = 20)
    public short LineWidth;

    @ByteOffset( offset = 22)
    public short DiamFlags;

    @ByteOffset( offset = 24)
    public int ServerObjectId;

    @ByteOffset( offset = 28)
    public int Height;

    @Override
    public String toString() {
        return "TOcadObject{" +
            "TDPoly_offset=" + TDPOLY_OFFSET +
            ", TDPoly_size=" + TDPOLY_SIZE +
            ", isLineCache=" + isLineCache +
            ", isSlope_cache=" + isSlope_cache +
            ", line_type_cache=" + line_type_cache +
            ", Sym=" + Sym +
            ", Otp=" + Otp +
            ", _Customer=" + _Customer +
            ", Ang=" + Ang +
            ", nItem=" + nItem +
            ", nText=" + nText +
            ", Mark=" + Mark +
            ", SnappingMark=" + SnappingMark +
            ", Col=" + Col +
            ", LineWidth=" + LineWidth +
            ", DiamFlags=" + DiamFlags +
            ", ServerObjectId=" + ServerObjectId +
            ", Height=" + Height +
            '}';
    }
}
