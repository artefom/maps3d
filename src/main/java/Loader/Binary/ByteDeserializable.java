package Loader.Binary;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * Created by Artem on 21.07.2016.
 */
public class ByteDeserializable {

    protected ByteBuffer readbb(SeekableByteChannel s, int bytes) {
        ByteBuffer bb = ByteBuffer.allocate(bytes);
        bb.flip();
        return (ByteBuffer)bb.flip();
    }

    SeekableByteChannel s;
    ByteBuffer buf;

    protected void readToBuffer(int offset) {
        try {
            s.position(offset);
            buf.clear();
            buf.position(0);
            s.read(buf);
            buf = (ByteBuffer) buf.rewind();
        } catch (Exception ex) {
            throw new RuntimeException("Invalid file format");
        }
    }

    protected long readLong(int offset) {
        readToBuffer(offset);
        return buf.asLongBuffer().get();
    }

    protected int readInt(int offset) {
        readToBuffer(offset);
        return buf.asIntBuffer().get();
    }

    protected short readShort(int offset) {
        readToBuffer(offset);
        return buf.asShortBuffer().get();
    }

    protected byte readByte(int offset) {
        readToBuffer(offset);
        return buf.get();
    }

    protected Object readObject(int offset, Class c) throws IllegalAccessException, InstantiationException {
        Object obj = c.newInstance();
        if ( ByteDeserializable.class.isInstance(obj) ) {
            ByteDeserializable bd = (ByteDeserializable)obj;
            bd.Deserialize(this.s, offset, buf);
            bd.buf = null;
            bd.s = null;
        } else {
            _deserialize(offset,obj);
        }
        return obj;
    }

    protected byte[] readByteArray(int offset, int size, int step) {
        byte[] b1 = new byte[size];
        for (int i = 0; i != size; ++i) {
            b1[i] = readByte(offset);
            offset += step;
        }
        return b1;
    }

    protected Object[] readObjectArray(int offset, int size, int step, Class c) throws IllegalAccessException, InstantiationException {
        Object[] ret = new Object[size];
        for (int i = 0; i != size; ++i) {
            ret[i] = readObject(offset,c);
            offset+=step;
        }
        return ret;
    }


    protected void _deserialize(int offset, Object obj, Field field) throws IllegalAccessException, InstantiationException {

        Class cls = field.getType();

        if (cls.isArray()) {
            serializedarray sz = field.getDeclaredAnnotation(serializedarray.class);
            int size = sz.size();
            int step = sz.step();
            Class comp_type = cls.getComponentType();
            if (long.class.isAssignableFrom(comp_type)) {
                //field.setLong( obj, readLong(offset) );
            } else if (int.class.isAssignableFrom(comp_type)) {
                //field.setInt( obj, readInt(offset) );
            } else if (short.class.isAssignableFrom(comp_type)) {
                //field.setShort( obj, readShort(offset) );
            } else if (byte.class.isAssignableFrom(comp_type)) {
                field.set(obj, readByteArray(offset, size, step));
                //field.setByte( obj, readByte(offset) );
            } else if (String.class.isAssignableFrom(comp_type)) {
                throw new RuntimeException("Can't assign string!");
            } else {
                Object[] objects = readObjectArray(offset,size,step,comp_type);
                Object arr = Array.newInstance(comp_type,size);
                Object[] arr_obj = (Object[])arr;
                for (int i = 0; i != objects.length; ++i) {
                    arr_obj[i] = objects[i];
                }
                field.set(obj, arr);
            }
        } else {

            if (long.class.isAssignableFrom(cls)) {
                field.setLong(obj, readLong(offset));
            } else if (int.class.isAssignableFrom(cls)) {
                field.setInt(obj, readInt(offset));
            } else if (short.class.isAssignableFrom(cls)) {
                field.setShort(obj, readShort(offset));
            } else if (byte.class.isAssignableFrom(cls)) {
                field.setByte(obj, readByte(offset));
            } else if (String.class.isAssignableFrom(cls)) {
                throw new RuntimeException("Can't assign string!");
            } else {
                field.set(obj,readObject(offset,cls));
                //throw new RuntimeException("Unknown type!");
            }
        }

    }

    protected void _deserialize(int global_offset, Object obj) {
        Field[] fields = obj.getClass().getFields();

        for (int i = 0; i != fields.length; ++i) {
            Field field = fields[i];

            byteoffset ofs = field.getDeclaredAnnotation(byteoffset.class);
            if (ofs != null) {
                int offset = ofs.offset() + global_offset;
                try {
                    _deserialize(offset, obj, field);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Unexpected case");
                } catch (InstantiationException e) {
                    throw new RuntimeException("Could not initialize field of class");
                }
            }
        }
    }

    public void Deserialize(SeekableByteChannel s,int offset, ByteBuffer buf) {
        this.s = s;
        this.buf = buf;
        _deserialize(offset,this);
    }

    public void Deserialize(SeekableByteChannel s, int offset) {
        this.s = s;
        buf = ByteBuffer.allocateDirect(8);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);
        Deserialize(s,offset,buf);
        this.s = null;
        this.buf = null;
    }
}
