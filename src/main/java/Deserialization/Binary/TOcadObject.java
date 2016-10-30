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

    // Flags for public use. Use them in coupe with getFlags()
    // As may be seen, yFlags are upper bits and xFlags are lower.
    public static int BEZIER_FIRST	= (1     );
    public static int BEZIER_SECOND	= (1 << 1);
    public static int EMPTY_LEFT 		= (1 << 2);
    public static int AREA_BORDER	 	= (1 << 3);
    public static int CONNER			  = (1 << 4);
    public static int HOLE_FIRST		= (1 << 5);
    public static int EMPTY_RIGHT 	    = (1 << 6);
    public static int IS_DASH	 		= (1 << 7);

    public int getFlags() {
        return (x&((1 << 4)-1)) | ((y&((1 << 4)-1))<<4);
    }

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

    public boolean isLine() {
        return getType() != -1 && getType() != 5;
    }

    private int isSlopeCache = 0;
    public boolean isSlope() {
        if (isSlopeCache == 0) {
            isSlopeCache = PropertiesLoader.ocad_input.isSlope(Sym) ? 1 : -1;
        }
        return isSlopeCache == 1;
    }

    private int isBorderCache = 0;
    public boolean isBorder() {
        if (isBorderCache == 0) {
            isBorderCache = PropertiesLoader.ocad_input.isBorder(Sym) ? 1 : -1;
        }
        return isBorderCache == 1;
    }

    private int lineTypeCache = -2;
    public int getType() {
        if (lineTypeCache == -2) {
            lineTypeCache = PropertiesLoader.ocad_input.getLineType(Sym);
        }
        return lineTypeCache;
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
