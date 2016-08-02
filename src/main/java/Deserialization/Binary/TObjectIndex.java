package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class TObjectIndex {

    @byteoffset( offset = 0)
    public LRect rc;

    @byteoffset( offset = 16)
    public int Pos;

    @byteoffset( offset = 20)
    public int Len;

    @byteoffset( offset = 24)
    public int Sym;

    @byteoffset( offset = 28)
    public byte ObjType;

    @byteoffset( offset = 29)
    public byte EncryptedMode;

    @byteoffset( offset = 30)
    public byte Status;

    @byteoffset( offset = 31)
    public byte ViewType;

    @byteoffset( offset = 32)
    public short Color;

    @byteoffset( offset = 34)
    public short Group;

    @byteoffset( offset = 36)
    public short ImpLayer;

    @byteoffset( offset = 38)
    public byte LayoutFont;

    @byteoffset( offset = 39)
    public byte Res2;

}
