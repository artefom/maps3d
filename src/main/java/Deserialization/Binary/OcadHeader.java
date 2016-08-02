package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class OcadHeader extends ByteDeserializable {
    @byteoffset( offset = 0)
    public short OCADMark;

    @byteoffset( offset = 2)
    public byte FileType;

    @byteoffset( offset = 3)
    public byte FileStatus;

    @byteoffset( offset = 4)
    public short Version;

    @byteoffset( offset = 6)
    public byte Subversion;

    @byteoffset( offset = 7)
    public byte SubSubversion;

    @byteoffset( offset = 8)
    public int FirstSymbolIndexBlk;

    @byteoffset( offset = 12)
    public int ObjectIndexBlock;

    @byteoffset( offset = 16)
    public int OfflineSyncSerial;

    @byteoffset( offset = 20)
    public int CurrentFileVersion;

    @byteoffset( offset = 24)
    public int Res2;

    @byteoffset( offset = 28)
    public int Res3;

    @byteoffset( offset = 32)
    public int FirstStringIndexBlk;
}
