package Utils;

import java.util.Iterator;

/**
 * Reversed iterator over primitive array
 */
public class ArrayReverseIterator<T> implements Iterator<T> {

    private T[] array;
    private int index;
    public ArrayReverseIterator( T[] array ) {
        this.array = array;
        index = array.length-1;
    }

    @Override
    public boolean hasNext() {
        return index >= 0;
    }

    @Override
    public T next() {
        return array[index--];
    }

}
