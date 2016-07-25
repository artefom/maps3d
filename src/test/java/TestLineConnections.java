import Algorithm.LineConnection.*;
import TestUtils.TestUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Before;
import org.junit.Test;
import static TestUtils.TestUtils.*;

import static org.junit.Assert.*;

/**
 * Created by Artem on 16.07.2016.
 */
public class TestLineConnections {

    //IsolineContainer container;
    GeometryFactory gf;

    public TestLineConnections() {
        gf = new GeometryFactory();
    }


//    private LineSegment createLineSegment(String str) {
//        List<Coordinate> coordinates = new ArrayList<>();
//        String[] tokens = str.split(",");
//        for (int i = 0; i != tokens.length; ++i) {
//            String[] subtoken = tokens[i].trim().split("\\s+");
//            double x = Double.parseDouble(subtoken[0]);
//            double y = Double.parseDouble(subtoken[1]);
//            coordinates.add( new Coordinate(x,y));
//        };
//        return new LineSegment(coordinates.get(0), coordinates.get(1));
//    }
//
//    private LineString createLineString(String str){
//
//
//        List<Coordinate> coordinates = new ArrayList<>();
//        String[] tokens = str.split(",");
//        for (int i = 0; i != tokens.length; ++i) {
//            String[] subtoken = tokens[i].trim().split("\\s+");
//            double x = Double.parseDouble(subtoken[0]);
//            double y = Double.parseDouble(subtoken[1]);
//            coordinates.add( new Coordinate(x,y));
//        };
//        return new LineString(
//                gf.getCoordinateSequenceFactory().create(
//                        coordinates.toArray(
//                                new Coordinate[coordinates.size()]
//                        )
//                ),
//                gf);
//    }
//
//    private Isoline createIsoline(int type, int side, String str){
//
//
//        List<Coordinate> coordinates = new ArrayList<>();
//        String[] tokens = str.split(",");
//        for (int i = 0; i != tokens.length; ++i) {
//            String[] subtoken = tokens[i].trim().split("\\s+");
//            double x = Double.parseDouble(subtoken[0]);
//            double y = Double.parseDouble(subtoken[1]);
//            coordinates.add( new Coordinate(x,y));
//        };
//        return new Isoline(
//                type,
//                side,
//                gf.getCoordinateSequenceFactory().create(
//                        coordinates.toArray(
//                                new Coordinate[coordinates.size()]
//                        )
//                ),
//                gf
//        );
//    }

    LineEnd
    is1_lbeg,       is1_lend,
    is1_lbeg_copy,  is1_lend_copy,

    is2_lbeg,       is2_lend,
    is2_lbeg_copy,  is2_lend_copy,

    is3_lbeg,       is3_lend,
    is3_lbeg_copy,  is3_lend_copy,

    is4_lbeg,       is4_lend,
    is4_lbeg_copy,  is4_lend_copy,

    is5_lbeg,       is5_lend,
    is5_lbeg_copy,  is5_lend_copy,

    is6_lbeg,       is6_lend,
    is6_lbeg_copy,  is6_lend_copy;

    Isoline_attributed is1,is2,is3,is4,is5,is6,
    is4_t1_s1,
    is4_t1_s2,
    is5_t1_s1,
    is5_t1_s2;

    Connection
    c_is1_end_s2_beg,
    c_is1_beg_is3_end,
    c_is1_end_is3_beg,
    c_is1_beg_is3_beg,
    c_is1_end_is3_end,
    c_is1_beg_is3_end_swapped,
    c_is5_end_is6_end,
    c_is3_end_is4_beg;

    @Before
    public void createIsolines() throws Exception {

//        container = new IsolineContainer( gf );
        is1 = createIsoline(0,0,"0 0.1,31 1,3 4",gf);
        is2 = createIsoline(1, -1, "-2 3, 0 12, 3 -3, -8 12",gf);
        is3 = createIsoline(0, 0, "0 0, 1 1, 2 2",gf);
        is4 = createIsoline(0,0,"2 2, 1 1, 0 0",gf);
        is4_t1_s1 = createIsoline(1,1,"2 2, 1 1, 0 0",gf);
        is4_t1_s2 = createIsoline(1,-1,"2 2, 1 1, 0 0",gf);
        is5 = createIsoline(1, 0, "0 0, 1 1, 2 2",gf);
        is5_t1_s1 = createIsoline(1, 1, "0 0, 1 1, 2 2",gf);
        is5_t1_s2 = createIsoline(1, -1, "0 0, 1 1, 2 2",gf);
        is6 = createIsoline(1, 0, "0 0, 1 1, 2 2",gf);

        is1_lbeg = LineEnd.fromIsoline(is1,1);
        is1_lend = LineEnd.fromIsoline(is1,-1);
        is1_lbeg_copy = LineEnd.fromIsoline(is1,1);
        is1_lend_copy = LineEnd.fromIsoline(is1,-1);

        is2_lbeg = LineEnd.fromIsoline(is2,1);
        is2_lend = LineEnd.fromIsoline(is2,-1);
        is2_lbeg_copy = LineEnd.fromIsoline(is2,1);
        is2_lend_copy = LineEnd.fromIsoline(is2,-1);

        is3_lbeg = LineEnd.fromIsoline(is3,1);
        is3_lend = LineEnd.fromIsoline(is3,-1);
        is3_lbeg_copy = LineEnd.fromIsoline(is3,1);
        is3_lend_copy = LineEnd.fromIsoline(is3,-1);

        is4_lbeg = LineEnd.fromIsoline(is4,1);
        is4_lend = LineEnd.fromIsoline(is4,-1);
        is4_lbeg_copy = LineEnd.fromIsoline(is4,1);
        is4_lend_copy = LineEnd.fromIsoline(is4,-1);

        is5_lbeg = LineEnd.fromIsoline(is5,1);
        is5_lend = LineEnd.fromIsoline(is5,-1);
        is5_lbeg_copy = LineEnd.fromIsoline(is5,1);
        is5_lend_copy = LineEnd.fromIsoline(is5,-1);

        is6_lbeg = LineEnd.fromIsoline(is6,1);
        is6_lend = LineEnd.fromIsoline(is6,-1);
        is6_lbeg_copy = LineEnd.fromIsoline(is6,1);
        is6_lend_copy = LineEnd.fromIsoline(is6,-1);

        c_is1_beg_is3_end = Connection.fromLineEnds(is1_lbeg,is3_lend);
        c_is1_beg_is3_end_swapped = Connection.fromLineEnds(is3_lend,is1_lbeg);
        c_is1_end_is3_beg = Connection.fromLineEnds(is1_lend,is3_lbeg);
        c_is1_beg_is3_beg = Connection.fromLineEnds(is1_lbeg,is3_lbeg);
        c_is1_end_is3_end = Connection.fromLineEnds(is1_lend,is3_lend);
        c_is3_end_is4_beg = Connection.fromLineEnds(is3_lend,is4_lbeg);
        c_is5_end_is6_end = Connection.fromLineEnds(is5_lend,is6_lbeg);
//
//        container.add(is1);
//        container.add(is2);
//        container.add(is3);
//        container.add(is4);
//        container.add(is5);
//        container.add(is6);
    }

    @Test
    public void testLineEquity() {

        assertTrue(is1 == is1);
        assertFalse(is1 == is2);
        assertNotEquals(is1, is2);
        assertNotEquals(is1, is3);
        assertNotEquals(is1, is4);
        assertNotEquals(is1, is5);

        assertNotEquals(is2, is3);
        assertNotEquals(is2, is4);
        assertNotEquals(is2, is5);

        TestUtils.assertEquals(is3, is4);
        assertEquals(is4, is3);
        assertEquals(is3.hashCode(), is4.hashCode());

        assertNotEquals(is4_t1_s1, is5_t1_s1);
        assertNotEquals(is4_t1_s2, is5_t1_s2);

        assertNotEquals(is4_t1_s1.hashCode(), is5_t1_s1.hashCode());
        assertNotEquals(is4_t1_s2.hashCode(), is5_t1_s2.hashCode());

        assertEquals(is4_t1_s1, is5_t1_s2);
        assertEquals(is4_t1_s2, is5_t1_s1);
        assertEquals(is4_t1_s1.hashCode(), is5_t1_s2.hashCode());
        assertEquals(is4_t1_s2.hashCode(), is5_t1_s1.hashCode());

        assertNotEquals(is3, is5);

        assertNotEquals(is4,is5);
    }

    @Test
    public void testLineEnds() throws Exception {

        assertEquals(is5, is6);
        assertNotEquals(is6_lbeg, is5_lbeg);
        assertNotEquals(is6_lend, is5_lend);
        assertNotEquals(is6_lbeg, is5_lbeg_copy);
        assertNotEquals(is6_lend_copy, is5_lend);
        assertEquals(is6_lbeg.line, is5_lbeg.line);
        assertNotEquals(is6_lbeg, is5_lbeg);

        assertEquals(is1_lbeg.end_index, 1);
//        assertEquals(is1_lbeg.line, TestUtils.createLineSegment("31.0 1.0, 0.0 0.1"));
        assertEquals(is1_lend.isoline,is1);
        assertEquals(is1_lend.end_index,-1);
//        assertEquals(is1_lend.line, TestUtils.createLineSegment("31 1, 3 4"));
        assertEquals(is1_lend.isoline,is1);

        assertEquals(is2_lbeg.end_index,1);
//        assertEquals(is2_lbeg.line, TestUtils.createLineSegment("0 12, -2 3"));
        assertEquals(is2_lbeg.isoline,is2);
        assertEquals(is2_lend.end_index,-1);
//        assertEquals(is2_lend.line, TestUtils.createLineSegment("3 -3, -8 12"));
        assertEquals(is2_lend.isoline,is2);

        assertEquals(is3_lbeg.end_index,1);
//        assertEquals(is3_lbeg.line, TestUtils.createLineSegment("1 1, 0 0"));
        assertEquals(is3_lbeg.isoline,is3);
        assertEquals(is3_lend.end_index,-1);
//        assertEquals(is3_lend.line, TestUtils.createLineSegment("1 1, 2 2"));
        assertEquals(is3_lend.isoline,is3);

        assertEquals(is4_lbeg.end_index,1);
//        assertEquals(is4_lbeg.line, TestUtils.createLineSegment("1 1, 2 2"));
        assertEquals(is4_lbeg.isoline,is4);
        assertEquals(is4_lend.end_index,-1);
//        assertEquals(is4_lend.line, TestUtils.createLineSegment("1 1, 0 0"));
        assertEquals(is4_lend.isoline,is4);

        assertEquals(is5_lbeg.end_index,1);
//        assertEquals(is5_lbeg.line, TestUtils.createLineSegment("1 1, 0 0"));
        assertEquals(is5_lbeg.isoline,is5);
        assertEquals(is5_lend.end_index,-1);
//        assertEquals(is5_lend.line, TestUtils.createLineSegment("1 1, 2 2"));
        assertEquals(is5_lend.isoline,is5);

        assertEquals(is6_lbeg.end_index, 1);
//        assertEquals(is6_lbeg.line, TestUtils.createLineSegment("1 1, 0 0"));
        assertEquals(is6_lbeg.isoline, is6);
        assertEquals(is6_lend.end_index,-1);
//        assertEquals(is6_lend.line, TestUtils.createLineSegment("1 1, 2 2"));
        assertEquals(is6_lend.isoline, is6);

        assertNotEquals(is1_lbeg, is1_lend);
        assertNotEquals(is4_lbeg, is1_lbeg);
    }

    @Test
    public void testConnections() throws Exception {

        assertEquals(c_is1_beg_is3_end,c_is1_beg_is3_end_swapped);
        assertEquals(c_is1_beg_is3_end.hashCode(), c_is1_beg_is3_end_swapped.hashCode());
        LineWelder lw = new LineWelder(gf,null);

        /*
        is1 = TestUtils.TestUtils.createIsoline(0,0,"0 0.1,31 1,3 4",gf);
        is2 = TestUtils.TestUtils.createIsoline(1, 2, "-2 3, 0 12, 3 -3, -8 12",gf);
        is3 = TestUtils.TestUtils.createIsoline(0, 0, "0 0, 1 1, 2 2",gf);
         */
        TestUtils.assertNotEquals(lw.Weld_copy(c_is1_beg_is3_end),
                createIsoline(0, 0, "3 4,31 1,0 0,0 2, 1 1, 0 0",gf));

        TestUtils.assertEquals(lw.Weld_copy(c_is1_beg_is3_end_swapped),
                lw.Weld_copy(c_is1_beg_is3_end));

        TestUtils.assertEquals(lw.Weld_copy(c_is1_beg_is3_end),
                createIsoline(0, 0, "0 0, 1 1, 2 2, 0 0.1, 31 1, 3 4",gf));
        TestUtils.assertEquals(lw.Weld_copy(c_is1_end_is3_beg),
                createIsoline(0, 0, "2 2, 1 1, 0 0, 3 4, 31 1, 0 0.1",gf));
        TestUtils.assertEquals(lw.Weld_copy(c_is1_end_is3_end),
                createIsoline(0, 0, "0 0, 1 1, 2 2, 3 4, 31 1, 0 0.1",gf));
        TestUtils.assertEquals(lw.Weld_copy(c_is1_beg_is3_beg),
                createIsoline(0, 0, "2 2, 1 1, 0 0, 0 0.1, 31 1, 3 4",gf));
    }

    private Connection createConnection(Coordinate start1, Coordinate end1,
                                                          Coordinate start2, Coordinate end2) {
        LineEnd le1 = TestUtils.createLineEnd(start1,end1,gf);
        LineEnd le2 = TestUtils.createLineEnd(start2,end2,gf);
        Connection con = Connection.fromLineEnds(le1,le2);
        return con;
    }

    @Test
    public void testLineAlignment1() {
        ConnectionEvaluator evaluator = new ConnectionEvaluator(0,Math.PI*90/180,0.2,0.03);
        Connection con = createConnection(
                new Coordinate(0,0),
                new Coordinate(1,0),
                new Coordinate(2,2),
                new Coordinate(1,1)
        );
        double score = evaluator.apply(con);
//        assertEquals(score,-2.25,0.0000001);

        con = createConnection(
                new Coordinate(0,0),
                new Coordinate(1,0),
                new Coordinate(3,0),
                new Coordinate(1.002,0)
        );
        score = evaluator.apply(con);
//        assertEquals(score,1,0.00000001);
    }

    public void testLineAlignment2() {
        ConnectionEvaluator evaluator = new ConnectionEvaluator(0,Math.PI*135/180,0.2,0.03);
        Connection con = createConnection(
                new Coordinate(0,0),
                new Coordinate(1,0),
                new Coordinate(2,2),
                new Coordinate(1,1)
        );
        double score = evaluator.apply(con);
//        assertEquals(score,-2,0.0000001);

        con = createConnection(
                new Coordinate(0,0),
                new Coordinate(1,0),
                new Coordinate(3,0),
                new Coordinate(1.002,0)
        );
        score = evaluator.apply(con);
  //      assertEquals(score,1,0.00000001);
    }

}
