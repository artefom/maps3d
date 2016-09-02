package Utils;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;

/**
 * Created by Artyom.Fomenko on 31.08.2016.
 */
public class TriangleUtils {

    public static void getCenter(ArrayList<Coordinate> vertexes, int[] triangle, Coordinate out) {
        out.x = getCenterX(vertexes,triangle);
        out.y = getCenterY(vertexes,triangle);
    }

    public static double getCenterX(ArrayList<Coordinate> vertexes, int[] triangle) {
        return (vertexes.get(triangle[0]).x+vertexes.get(triangle[1]).x+vertexes.get(triangle[2]).x)/3;
    }

    public static double getCenterY(ArrayList<Coordinate> vertexes, int[] triangle) {
        return (vertexes.get(triangle[0]).y+vertexes.get(triangle[1]).y+vertexes.get(triangle[2]).y)/3;
    }

    public static void getCenter(Coordinate[] vertexes, int[] triangle, Coordinate out) {
        out.x = getCenterX(vertexes,triangle);
        out.y = getCenterY(vertexes,triangle);
    }

    public static double getCenterX(Coordinate[] vertexes, int[] triangle) {
        return (vertexes[triangle[0]].x+vertexes[triangle[1]].x+vertexes[triangle[2]].x)/3;
    }

    public static double getCenterY(Coordinate[] vertexes, int[] triangle) {
        return (vertexes[triangle[0]].y+vertexes[triangle[1]].y+vertexes[triangle[2]].y)/3;
    }
}
