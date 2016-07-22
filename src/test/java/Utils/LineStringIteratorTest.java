package Utils;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static TestUtils.TestUtils.*;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class LineStringIteratorTest {

    GeometryFactory gf = new GeometryFactory();

    @Test
    public void hasNextTest1() throws Exception {
        LineString ls = createLineString("0 0, 1 1", gf);
        LineSegment buf = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,buf);
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void hasNextTest2() throws Exception {
        LineString ls = createLineString("0 0, 4 0, 4 3, 0 0",gf);
        LineSegment buf = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,buf);
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void nextTest() throws Exception {
        LineString ls = createLineString("0 0, 4 0, 4 3, 0 0",gf);
        LineSegment buf = new LineSegment();
        LineStringIterator it = new LineStringIterator(ls,buf);
        it.next();
        System.out.println(buf);
        assertEquals(createLineSegment("0 0, 4 0"),buf);
        it.next();
        assertEquals(createLineSegment("4 0, 4 3"),buf);
        it.next();
        assertEquals(createLineSegment("4 3, 0 0"),buf);
    }

}