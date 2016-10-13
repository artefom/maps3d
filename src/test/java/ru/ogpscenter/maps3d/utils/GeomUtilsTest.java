package ru.ogpscenter.maps3d.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Test;

import static TestUtils.TestUtils.assertEquals;
import static TestUtils.TestUtils.createLineString;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class GeomUtilsTest {

    GeometryFactory gf = new GeometryFactory();

    @Test
    public void pointAlongTest() throws Exception {
        LineString ls = createLineString("0 0, 4 0, 4 3, 0 0",gf);
        assertEquals(new Coordinate(0,0), GeomUtils.pointAlong(ls,-1.0/12) );
        assertEquals(new Coordinate(0,0), GeomUtils.pointAlong(ls,0.0/12) );
        assertEquals(new Coordinate(1,0), GeomUtils.pointAlong(ls,1.0/12) );
        assertEquals(new Coordinate(2,0), GeomUtils.pointAlong(ls,2.0/12) );
        assertEquals(new Coordinate(3,0), GeomUtils.pointAlong(ls,3.0/12) );
        assertEquals(new Coordinate(4,0), GeomUtils.pointAlong(ls,4.0/12) );
        assertEquals(new Coordinate(4,1), GeomUtils.pointAlong(ls,5.0/12) );
        assertEquals(new Coordinate(4,2), GeomUtils.pointAlong(ls,6.0/12) );
        assertEquals(new Coordinate(4,3), GeomUtils.pointAlong(ls,7.0/12) );
        assertEquals(new Coordinate(4-4.0/5*1,3-3.0/5*1), GeomUtils.pointAlong(ls,8.0/12) );
        assertEquals(new Coordinate(4-4.0/5*2,3-3.0/5*2), GeomUtils.pointAlong(ls,9.0/12) );
        assertEquals(new Coordinate(4-4.0/5*3,3-3.0/5*3), GeomUtils.pointAlong(ls,10.0/12) );
        assertEquals(new Coordinate(4-4.0/5*4,3-3.0/5*4), GeomUtils.pointAlong(ls,11.0/12) );
        assertEquals(new Coordinate(4-4.0/5*5,3-3.0/5*5), GeomUtils.pointAlong(ls,12.0/12) );
        assertEquals(new Coordinate(4-4.0/5*5,3-3.0/5*5), GeomUtils.pointAlong(ls,13.0/12) );
    }

}