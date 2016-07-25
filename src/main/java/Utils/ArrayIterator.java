package Utils;

import java.util.Iterator;

/**
 * Created by Artem on 16.07.2016.
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
