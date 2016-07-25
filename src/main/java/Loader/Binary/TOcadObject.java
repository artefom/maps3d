package Loader.Binary;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;

/**
 * Created by Artem on 21.07.2016.
 */
public class TOcadObject extends ByteDeserializable {

    private final int TDPoly_offset = 32;
    private final int TDPoly_size = 8;
    public ArrayList<TDPoly> Poly;

    @Override
    public void Deserialize(SeekableByteChannel s, int offset, ByteBuffer buf) {
        super.Deserialize(s, offset, buf);
        Object[] obj;
        try {
            obj = readObjectArray(offset+TDPoly_offset, this.nItem+1, TDPoly_size, TDPoly.class);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid format");
        }
        this.Poly = new ArrayList<>();
        for (int i = 0; i != obj.length; ++i) {
            TDPoly poly = (TDPoly)obj[i];
            if (!poly.isNull()) {
                this.Poly.add(poly);
            }
        }
        this.nItem = this.Poly.size();
    }

    public boolean isLine() {
        return Sym == 101000 ||
                Sym == 102000 ||
                Sym == 103000 ||
                Sym == 106001 ||
                Sym == 106000 ||
                Sym == 106006;
    }

    public boolean isSlope() {
        return Sym == 104000;
    }

    public int getType() {
        if (Sym == 101000) return 2;
        if (Sym == 102000) return 3;
        if (Sym == 103000) return 1;
        if (Sym == 106001 || Sym == 106000 || Sym == 106006) return 4;
        return -1;
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
