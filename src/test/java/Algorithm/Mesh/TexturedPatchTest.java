package Algorithm.Mesh;

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by Artyom.Fomenko on 31.08.2016.
 */
public class TexturedPatchTest {

    Coordinate[] coordinates;
    ArrayList<int[]> triangles;
    TexturedPatch tp;

    @Before
    public void createTexturedPatch() {
        coordinates = new Coordinate[] {
                new Coordinate(0,0),
                new Coordinate(1,0),
                new Coordinate(1,1),
                new Coordinate(0,1),
                new Coordinate(0,2),
                new Coordinate(1,2),
        };

        triangles = new ArrayList<>();
        triangles.add(new int[] {0,1,3});
        triangles.add(new int[] {3,1,2});
        triangles.add(new int[] {3,2,4});
        triangles.add(new int[] {4,2,5});

        tp = TexturedPatch.fromTriangles(coordinates,triangles);
    }

    @Test
    public void UVtoXY() throws Exception {

    }

    @Test
    public void UVtoXY1() throws Exception {

    }

    @Test
    public void UVtoXY2() throws Exception {

    }

    @Test
    public void XYtoUV() throws Exception {
        Coordinate c = new Coordinate(0.5,2);
        System.out.println( tp.XYtoUV(c) );
    }

}