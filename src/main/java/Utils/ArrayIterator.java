package Utils;

import java.util.Iterator;

/**
 * Iterator over primitive array
 */
public class ArrayIterator<T> implements Iterator<T> {

    private T[] array;
    private int index;
    public ArrayIterator(T[] array) {
        this.array = array;
        index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public T next() {
        return array[index++];
    }
}
