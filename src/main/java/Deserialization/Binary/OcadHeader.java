package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class OcadHeader extends ByteDeserializable {
    @ByteOffset( offset = 0)
    public short OCADMark;

    @ByteOffset( offset = 2)
    public byte FileType;

    @ByteOffset( offset = 3)
    public byte FileStatus;

    @ByteOffset( offset = 4)
    public short Version;

    @ByteOffset( offset = 6)
    public byte Subversion;

    @ByteOffset( offset = 7)
    public byte SubSubversion;

    @ByteOffset( offset = 8)
    public int FirstSymbolIndexBlk;

    @ByteOffset( offset = 12)
    public int ObjectIndexBlock;

    @ByteOffset( offset = 16)
    public int OfflineSyncSerial;

    @ByteOffset( offset = 20)
    public int CurrentFileVersion;

    @ByteOffset( offset = 24)
    public int Res2;

    @ByteOffset( offset = 28)
    public int Res3;

    @ByteOffset( offset = 32)
    public int FirstStringIndexBlk;

    @Override
    public String toString() {
        return "OcadHeader{" +
            "OCADMark=" + OCADMark +
            ", FileType=" + FileType +
            ", FileStatus=" + FileStatus +
            ", Version=" + Version +
            ", Subversion=" + Subversion +
            ", SubSubversion=" + SubSubversion +
            ", FirstSymbolIndexBlk=" + FirstSymbolIndexBlk +
            ", ObjectIndexBlock=" + ObjectIndexBlock +
            ", OfflineSyncSerial=" + OfflineSyncSerial +
            ", CurrentFileVersion=" + CurrentFileVersion +
            ", Res2=" + Res2 +
            ", Res3=" + Res3 +
            ", FirstStringIndexBlk=" + FirstStringIndexBlk +
            '}';
    }
}
