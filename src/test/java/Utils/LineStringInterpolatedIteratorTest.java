package Utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Test;

import static TestUtils.TestUtils.assertEquals;
import static TestUtils.TestUtils.createLineSegment;
import static TestUtils.TestUtils.createLineString;
import org.junit.Assert;
//import static org.junit.Assert.*;
//import static org.junit.Assert.assertEquals;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class LineStringInterpolatedIteratorTest {

    GeometryFactory gf = new GeometryFactory();

    @Test
    public void hasNextTest1() throws Exception {
        LineString ls = createLineString("0 0, 1 1", gf);
        LineSegment buf = new LineSegment();
        LineStringInterpolatedIterator it = new LineStringInterpolatedIterator(ls,buf,5);
        Assert.assertTrue(it.hasNext());
        it.next();
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void hasNextTest2() throws Exception {
        LineString ls = createLineString("0 0, 4 0, 4 3, 0 0", gf);
        LineSegment buf = new LineSegment();
        LineStringInterpolatedIterator it = new LineStringInterpolatedIterator(ls,buf,12.0/4);
        Assert.assertTrue(it.hasNext());
        it.next();
        Assert.assertTrue(it.hasNext());
        it.next();
        Assert.assertTrue(it.hasNext());
        it.next();
        Assert.assertTrue(it.hasNext());
        it.next();
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void nextTest1() throws Exception {
        LineString ls = createLineString("0 0, 5 0", gf);
        LineSegment buf = new LineSegment();
        LineStringInterpolatedIterator it = new LineStringInterpolatedIterator(ls,buf,1);

        it.next();
        assertEquals(createLineSegment("0 0, 1 0"),buf);
        it.next();
        assertEquals(createLineSegment("1 0, 2 0"),buf);
        it.next();
        assertEquals(createLineSegment("2 0, 3 0"),buf);
        it.next();
        assertEquals(createLineSegment("3 0, 4 0"),buf);
        it.next();
        assertEquals(createLineSegment("4 0, 5 0"),buf);
    }

    @Test
    public void nextTest2() throws Exception {
        LineString ls = createLineString("0 0, 4 0, 4 3, 0 0", gf);
        LineSegment buf = new LineSegment();
        LineStringInterpolatedIterator it = new LineStringInterpolatedIterator(ls,buf,1);

        it.next();
        assertEquals(createLineSegment("0 0, 1 0"),buf);
        it.next();
        assertEquals(createLineSegment("1 0, 2 0"),buf);
        it.next();
        assertEquals(createLineSegment("2 0, 3 0"),buf);
        it.next();
        assertEquals(createLineSegment("3 0, 4 0"),buf);
        it.next();
        assertEquals(createLineSegment("4 0, 4 1"),buf);
        it.next();
        assertEquals(createLineSegment("4 1, 4 2"),buf);
        it.next();
        assertEquals(createLineSegment("4 2, 4 3"),buf);
    }

    @Test
    public void nextTest3() throws Exception {
        LineString ls = createLineString("0 0, 4 0, 4 3, 12 3, 43 1, -10 -300, 10 10, 5 5, -5 -5", gf);
        LineSegment buf = new LineSegment();
        LineStringInterpolatedIterator it = new LineStringInterpolatedIterator(ls,buf,ls.getLength()/100);
        double length = ls.getLength();
        double step = 1.0/100;
        int i = 0;
        while (it.hasNext()) {
            it.next();
            Coordinate truth_p = GeomUtils.pointAlong(ls,i*step);
            Coordinate test_p = buf.p0;
            assertEquals(truth_p,test_p);
            i += 1;
        }
    }

    @Test
    public void nextTest4() throws Exception {
        LineString ls = createLineString("0 0, 2 2", gf);
        double length = ls.getLength();

        LineSegment buf = new LineSegment();

        LineStringInterpolatedIterator it = new LineStringInterpolatedIterator(ls,buf,length/10);
        double step = 1.0/10;
        int i = 0;
        while (it.hasNext()) {
            it.next();
            Coordinate truth_p = GeomUtils.pointAlong(ls,i*step);
            Coordinate test_p = buf.p0;
            assertEquals(truth_p,test_p);
            i += 1;
        }
    }

}