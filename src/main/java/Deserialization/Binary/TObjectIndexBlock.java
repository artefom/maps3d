package Deserialization.Binary;

import java.util.Arrays;

/**
 * Created by Artem on 21.07.2016.
 */
public class TObjectIndexBlock extends ByteDeserializable {

    @byteoffset(offset = 0)
    public int NextObjectIndexBlock; 		// offset of next index block

    @serializedarray(size = 256, step = 40)
    @byteoffset(offset = 4)
    public TObjectIndex[] Table;

    @Override
    public String toString() {
        return "TObjectIndexBlock{" +
            "NextObjectIndexBlock=" + NextObjectIndexBlock +
            '}';
    }
}
