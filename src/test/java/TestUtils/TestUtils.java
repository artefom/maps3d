package TestUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Assert;
import ru.ogpscenter.maps3d.algorithm.repair.AttributedIsoline;
import ru.ogpscenter.maps3d.algorithm.repair.LineEnd;
import ru.ogpscenter.maps3d.isolines.Isoline;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class TestUtils {

    public static LineSegment createLineSegment(String str) {
        List<Coordinate> coordinates = new ArrayList<>();
        String[] tokens = str.split(",");
        for (int i = 0; i != tokens.length; ++i) {
            String[] subtoken = tokens[i].trim().split("\\s+");
            double x = Double.parseDouble(subtoken[0]);
            double y = Double.parseDouble(subtoken[1]);
            coordinates.add( new Coordinate(x,y));
        };
        return new LineSegment(coordinates.get(0), coordinates.get(1));
    }

    public static LineString createLineString(String str, GeometryFactory gf){


        List<Coordinate> coordinates = new ArrayList<>();
        String[] tokens = str.split(",");
        for (int i = 0; i != tokens.length; ++i) {
            String[] subtoken = tokens[i].trim().split("\\s+");
            double x = Double.parseDouble(subtoken[0]);
            double y = Double.parseDouble(subtoken[1]);
            coordinates.add( new Coordinate(x,y));
        };
        return new LineString(
                gf.getCoordinateSequenceFactory().create(
                        coordinates.toArray(
                                new Coordinate[coordinates.size()]
                        )
                ),
                gf);
    }

    public static AttributedIsoline createIsoline(int type, int side, String str, GeometryFactory gf){


        List<Coordinate> coordinates = new ArrayList<>();
        String[] tokens = str.split(",");
        for (int i = 0; i != tokens.length; ++i) {
            String[] subtoken = tokens[i].trim().split("\\s+");
            double x = Double.parseDouble(subtoken[0]);
            double y = Double.parseDouble(subtoken[1]);
            coordinates.add( new Coordinate(x,y));
        };
        return new AttributedIsoline( new Isoline(
                type,
                side,
                gf.getCoordinateSequenceFactory().create(
                        coordinates.toArray(
                                new Coordinate[coordinates.size()]
                        )
                ),
                gf
        ));
    }

    static double precision_tolerance = 0.00001;

    public static AttributedIsoline createIsoline(Coordinate start, Coordinate end,GeometryFactory gf) {
        AttributedIsoline il = new AttributedIsoline(
                new Isoline(0,0,gf.getCoordinateSequenceFactory().create(new Coordinate[] {start,end}),gf)
        );
        return il;
    }

    public static LineEnd createLineEnd(Coordinate start, Coordinate end, GeometryFactory gf) {
        return createIsoline(start,end,gf).end;
    }

    public static void assertEquals(Coordinate c1, Coordinate c2) {
        Assert.assertEquals(c1.x,c2.x,precision_tolerance);
        Assert.assertEquals(c1.y,c2.y,precision_tolerance);
    }

    public static void assertNotEquals(Coordinate c1, Coordinate c2) {
        Assert.assertNotEquals(c1.x,c2.x,precision_tolerance);
        Assert.assertNotEquals(c1.y,c2.y,precision_tolerance);
    }

    public static void assertEquals(LineSegment l1, LineSegment l2) {
        TestUtils.assertEquals(l1.p0,l2.p0);
        TestUtils.assertEquals(l2.p1,l2.p1);
    };

    public static void assertNotEquals(LineSegment l1, LineSegment l2) {
        TestUtils.assertNotEquals(l1.p0,l2.p0);
        TestUtils.assertNotEquals(l2.p1,l2.p1);
    };

    public static boolean equals(AttributedIsoline lhs, AttributedIsoline rhs) throws RuntimeException {
        if (lhs.getLineString().getNumPoints() != rhs.getLineString().getNumPoints()) return false;
        if (lhs.getType() != rhs.getType()) return false;

        boolean exact_match = true;
        boolean reversed_match = true;

        for (int i = 0; i != lhs.getLineString().getNumPoints(); ++i) {
            //if ( !lhs.getLineString().getCoordinateN(i).equals( lhs.getLineString().getCoordinateN(i) ) ) {
            Coordinate coord1 = lhs.getLineString().getCoordinateN(i);
            Coordinate coord2 = rhs.getLineString().getCoordinateN(i);
            if ( Math.abs(coord1.x - coord2.x) > precision_tolerance || Math.abs(coord2.y - coord2.y) > precision_tolerance ) {
                exact_match = false;
                break;
            }
        }

        for (int i = 0; i != lhs.getLineString().getNumPoints(); ++i) {
            //if (!lhs.getLineString().getCoordinateN(i).equals(rhs.getLineString().getCoordinateN(lhs.getLineString().getNumPoints()-1-i))) {

            Coordinate coord1 = lhs.getLineString().getCoordinateN(i);
            Coordinate coord2 = rhs.getLineString().getCoordinateN(rhs.getLineString().getNumPoints()-1-i);
            if ( Math.abs(coord1.x - coord2.x) > precision_tolerance || Math.abs(coord2.y - coord2.y) > precision_tolerance ) {
                reversed_match = false;
                break;
            }
        }

        if (exact_match) {
            if (lhs.getSlopeSide() == rhs.getSlopeSide()) return true;
        }
        if (reversed_match) {
            if (lhs.getSlopeSide() == -rhs.getSlopeSide()) return true;
        }

        //if (this.slope_side != other.slope_side) return false;
        return false;
    }

    public static void assertEquals(AttributedIsoline lhs, AttributedIsoline rhs) {
        Assert.assertTrue(equals(lhs,rhs));
    }

    public static void assertNotEquals(AttributedIsoline lhs, AttributedIsoline rhs) {
        Assert.assertFalse(equals(lhs,rhs));
    }

}
