package Algorithm;

import Algorithm.LineConnection.Connection;
import Algorithm.LineConnection.Isoline_attributed;
import Algorithm.LineConnection.LineEnd;
import Algorithm.LineConnection.LineWelder;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Before;
import org.junit.Test;

import static TestUtils.TestUtils.createIsoline;
import static org.junit.Assert.*;

/**
 * Created by Artem on 20.07.2016.
 */
public class LineWelderTest {

    private LineEnd
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

    private Isoline_attributed is1,is2,is3,is4,is5,is6,
            is4_t1_s1,
            is4_t1_s2,
            is5_t1_s1,
            is5_t1_s2;

    private Connection
            c_is1_beg_is2_beg,
            c_is1_beg_is3_end,
            c_is1_end_is3_beg,
            c_is1_beg_is3_beg,
            c_is1_end_is3_end,
            c_is1_beg_is3_end_swapped,
            c_is5_end_is6_end,
            c_is3_end_is4_beg;

    private GeometryFactory gf;
    private LineWelder welder;

    @Before
    public void setupIsolines() {
        gf = new GeometryFactory();
        welder = new LineWelder(gf,null);
        is1 = createIsoline(0,0,"0 0.1,31 1,3 4",gf);
        is2 = createIsoline(0, 0, "-2 3, 0 12, 3 -3, -8 12",gf);
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
        c_is1_beg_is2_beg = Connection.fromLineEnds(is1_lbeg,is2_lbeg);
    }

    @Test
    public void weld_copy() throws Exception {

    }

    @Test
    public void weldValidTest() throws Exception {
        assertTrue(c_is1_beg_is3_beg.isValid());
        assertTrue(c_is1_beg_is2_beg.isValid());

        System.out.println(is1);
        System.out.println(is3);
        Isoline_attributed is = welder.Weld(c_is1_beg_is3_beg);
        System.out.println(is);
        System.out.println(is.begin);
        System.out.println(is.end);
        assertFalse(is.begin.equals(is.end));
        assertFalse(is1.isValid());
        assertFalse(is3.isValid());
        assertTrue(is1.begin == null);
        assertTrue(is3.begin == null);
        assertTrue(is.begin.isValid());
        assertTrue(is.end.isValid());
        assertTrue(is.isValid());
        assertFalse(c_is1_beg_is3_beg.isValid());
        assertFalse(c_is1_beg_is2_beg.isValid());
    }

    @Test
    public void weldAll() throws Exception {

    }

}