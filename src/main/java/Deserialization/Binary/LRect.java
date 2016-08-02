package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class LRect {
    @byteoffset( offset = 0)
    public int Left;

    @byteoffset( offset = 4)
    public int Top;

    @byteoffset( offset = 8)
    public int Right;

    @byteoffset( offset = 12)
    public int Bottom;
}
