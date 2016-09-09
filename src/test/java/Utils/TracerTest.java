package Utils;

import TestUtils.TestUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Created by Artyom.Fomenko on 26.07.2016.
 */
public class TracerTest {

    private CachedTracer<LineString> tracer;
    private ArrayList<LineString> lines;
    private GeometryFactory gf;

    @Before
    public void initLines() {
        gf = new GeometryFactory();
        lines = new ArrayList<>();
        tracer = null;
    }

    private void initTracer() {

        tracer = new CachedTracer<>(lines,
                (x)->x,gf);
    }

    private LineString add(LineString ls) {
        lines.add(ls);
        return ls;
    }

    @Test
    public void traceTest1() {
        LineString line1 =  add(TestUtils.createLineString("1 -1, 1 1",gf));
        initTracer();
        CachedTracer.traceres res = tracer.trace(new Coordinate(2,0),new Vector2D(-1,0),0.001,1000);

        assertEquals(line1,res.entitiy);
        assertEquals(-1,res.side);
    }


    @Test
    public void traceTest2() {
        LineString line1 =  add(TestUtils.createLineString("1 -1, 1 1",gf));
        initTracer();
        CachedTracer.traceres res = tracer.trace(new Coordinate(0,0),new Vector2D(1,0),0.001,1000);

        assertEquals(line1,res.entitiy);
        assertEquals(1,res.side);
    }

}