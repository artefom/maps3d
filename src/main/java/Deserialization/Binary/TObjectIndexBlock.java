package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class TObjectIndexBlock extends ByteDeserializable {

    @byteoffset(offset = 0)
    public int NextObjectIndexBlock; 		// offset of next index block

    @serializedarray(size = 256, step = 40)
    @byteoffset(offset = 4)
    public TObjectIndex[] Table;

}
