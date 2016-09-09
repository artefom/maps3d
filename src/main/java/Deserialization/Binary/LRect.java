package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class LRect {
    @ByteOffset( offset = 0)
    public int Left;

    @ByteOffset( offset = 4)
    public int Top;

    @ByteOffset( offset = 8)
    public int Right;

    @ByteOffset( offset = 12)
    public int Bottom;
}
