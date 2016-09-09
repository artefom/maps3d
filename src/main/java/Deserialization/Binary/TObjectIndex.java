package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class TObjectIndex {

    @ByteOffset( offset = 0)
    public LRect rc;

    @ByteOffset( offset = 16)
    public int Pos;

    @ByteOffset( offset = 20)
    public int Len;

    @ByteOffset( offset = 24)
    public int Sym;

    @ByteOffset( offset = 28)
    public byte ObjType;

    @ByteOffset( offset = 29)
    public byte EncryptedMode;

    @ByteOffset( offset = 30)
    public byte Status;

    @ByteOffset( offset = 31)
    public byte ViewType;

    @ByteOffset( offset = 32)
    public short Color;

    @ByteOffset( offset = 34)
    public short Group;

    @ByteOffset( offset = 36)
    public short ImpLayer;

    @ByteOffset( offset = 38)
    public byte LayoutFont;

    @ByteOffset( offset = 39)
    public byte Res2;

    @Override
    public String toString() {
        return "TObjectIndex{" +
            "rc=" + rc +
            ", Pos=" + Pos +
            ", Len=" + Len +
            ", Sym=" + Sym +
            ", ObjType=" + ObjType +
            ", EncryptedMode=" + EncryptedMode +
            ", Status=" + Status +
            ", ViewType=" + ViewType +
            ", Color=" + Color +
            ", Group=" + Group +
            ", ImpLayer=" + ImpLayer +
            ", LayoutFont=" + LayoutFont +
            ", Res2=" + Res2 +
            '}';
    }
}
