package Deserialization.Binary;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;


/**
 * Created by Artem on 21.07.2016.
 */
public abstract class ByteDeserializable {

    protected ByteBuffer readbb(SeekableByteChannel s, int bytes) {
        ByteBuffer bb = ByteBuffer.allocate(bytes);
        bb.flip();
        return (ByteBuffer)bb.flip();
    }

    ByteBuffer buffer;

    protected Object readObject(int offset, Class c) throws IllegalAccessException, InstantiationException {
        Object obj = c.newInstance();
        if ( ByteDeserializable.class.isInstance(obj) ) {
            ByteDeserializable bd = (ByteDeserializable)obj;
            bd.deserialize(this.buffer, offset);
        } else {
            doDeserialize(offset,obj);
        }
        return obj;
    }

    protected byte[] readByteArray(int offset, int size, int step) {
        byte[] bytes = new byte[size];
        for (int i = 0; i != size; ++i) {
            bytes[i] = buffer.get(offset);
            offset += step;
        }
        return bytes;
    }

    protected Object[] readObjectArray(int offset, int size, int step, Class c) throws IllegalAccessException, InstantiationException {
        Object[] ret = new Object[size];
        for (int i = 0; i != size; ++i) {
            ret[i] = readObject(offset,c);
            System.out.println(ret[i].toString());
            offset+=step;
        }
        return ret;
    }


    protected void doDeserialize(int offset, Object obj, Field field) throws IllegalAccessException, InstantiationException {

        Class cls = field.getType();

        if (cls.isArray()) {
            SerializedArray sz = field.getDeclaredAnnotation(SerializedArray.class);
            int size = sz.size();
            int step = sz.step();
            Class comp_type = cls.getComponentType();
            if (long.class.isAssignableFrom(comp_type)) {
                throw new IllegalAccessException("Not implemented");
                //field.setLong( obj, readLong(offset) );
            } else if (int.class.isAssignableFrom(comp_type)) {
                throw new IllegalAccessException("Not implemented");
                //field.setInt( obj, readInt(offset) );
            } else if (short.class.isAssignableFrom(comp_type)) {
                throw new IllegalAccessException("Not implemented");
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
                field.setLong(obj, buffer.getLong(offset));
            } else if (int.class.isAssignableFrom(cls)) {
                field.setInt(obj, buffer.getInt(offset));
            } else if (short.class.isAssignableFrom(cls)) {
                field.setShort(obj, buffer.getShort(offset));
            } else if (byte.class.isAssignableFrom(cls)) {
                field.setByte(obj, buffer.get(offset));
            } else if (String.class.isAssignableFrom(cls)) {
                throw new RuntimeException("Can't assign string!");
            } else {
                field.set(obj,readObject(offset,cls));
                //throw new RuntimeException("Unknown type!");
            }
        }

    }

    protected void doDeserialize(int global_offset, Object obj) {
        Field[] fields = obj.getClass().getFields();

        for (int i = 0; i != fields.length; ++i) {
            Field field = fields[i];

            ByteOffset ofs = field.getDeclaredAnnotation(ByteOffset.class);
            if (ofs != null) {
                int offset = ofs.offset() + global_offset;
                try {
                    doDeserialize(offset, obj, field);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Unexpected case");
                } catch (InstantiationException e) {
                    throw new RuntimeException("Could not initialize field of class");
                }
            }
        }
    }

    public void deserialize(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        doDeserialize(offset, this);
    }
}
