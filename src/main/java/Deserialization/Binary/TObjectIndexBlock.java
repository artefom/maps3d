package Deserialization.Binary;

/**
 * Created by Artem on 21.07.2016.
 */
public class TObjectIndexBlock extends ByteDeserializable {

    @ByteOffset(offset = 0)
    public int NextObjectIndexBlock; 		// offset of next index block

    @SerializedArray(size = 256, step = 40)
    @ByteOffset(offset = 4)
    public TObjectIndex[] Table;

    @Override
    public String toString() {
        return "TObjectIndexBlock{" +
            "NextObjectIndexBlock=" + NextObjectIndexBlock +
            '}';
    }
}
