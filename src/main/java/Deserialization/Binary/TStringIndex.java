package Deserialization.Binary;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Artyom.Fomenko on 19.08.2016.
 */
public class TStringIndex extends ByteDeserializable {

    @byteoffset(offset = 0)
    public int Pos = 0;

    @byteoffset(offset = 4)
    public int Len = 0;

    @byteoffset(offset = 8)
    public int RecType = 0;

    @byteoffset(offset = 12)
    public int ObjIndex = 0;

    private String string;

    public String getString() {
        return string;
    }

    @Override
    public void Deserialize(SeekableByteChannel s, int offset, ByteBuffer buf)  {
        super.Deserialize(s, offset, buf);

        try {
            s.position(Pos);

            if (Len > 10000) {
                System.out.println("ALARM!");
            }
            ByteBuffer stringBuf = ByteBuffer.allocateDirect(Len);
            char[] chars = new char[Len];
            s.position(Pos);
            s.read(stringBuf);
            stringBuf = (ByteBuffer) stringBuf.rewind();

            int i = 0;
            for (i = 0; i != Len; ++i) {
                byte b = stringBuf.get();
                if (b == 0) break;
                chars[i] = (char)(b);
            }

            if (i == 0) {string = null; return;}

            char[] chars_shrunk = Arrays.copyOfRange(chars,0,i);

            string = new String(chars_shrunk);

        } catch (Exception ex) {
            string = null;
            return;
        }
    }

    @Override
    public String toString() {
        return "TStringIndex{" +
            "Pos=" + Pos +
            ", Len=" + Len +
            ", RecType=" + RecType +
            ", ObjIndex=" + ObjIndex +
            '}';
    }
}
