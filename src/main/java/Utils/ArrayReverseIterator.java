package Utils;

import java.util.Iterator;

/**
 * Created by Artem on 16.07.2016.
 */
public class ArrayReverseIterator<T> implements Iterator<T> {

    T[] array;
    int index;
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
