package Deserialization.Binary;

/**
 * Created by Artyom.Fomenko on 19.08.2016.
 */
public class TStringIndexBlock extends ByteDeserializable {

    @ByteOffset(offset = 0)
    public int NextIndexBlock; 		// offset of next index block

    @SerializedArray(size = 256, step = 16)
    @ByteOffset(offset = 4)
    public TStringIndex[] Table;

}
