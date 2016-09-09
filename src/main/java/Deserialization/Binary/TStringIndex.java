package Deserialization.Binary;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Artyom.Fomenko on 19.08.2016.
 */
public class TStringIndex extends ByteDeserializable {

    @ByteOffset(offset = 0)
    public int Pos = 0;

    @ByteOffset(offset = 4)
    public int Len = 0;

    @ByteOffset(offset = 8)
    public int RecType = 0;

    @ByteOffset(offset = 12)
    public int ObjIndex = 0;

    private String string;

    public String getString() {
        return string;
    }

    @Override
    public void deserialize(ByteBuffer s, int offset)  {
        super.deserialize(s, offset);

        try {
            s.position(Pos);
            if (Len > 10000) {
                System.out.println("Warning! String index is too big!");
            }
            char[] chars = new char[Len];
            s.position(Pos);
            byte[] byteBuf = new byte[Len];
            s.get(byteBuf,0,Len);
            ByteBuffer stringBuf = ByteBuffer.wrap(byteBuf);
            stringBuf = (ByteBuffer) stringBuf.rewind();

            int i;
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
        }
    }

}
