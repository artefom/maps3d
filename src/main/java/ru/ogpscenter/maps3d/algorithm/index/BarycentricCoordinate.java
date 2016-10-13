package ru.ogpscenter.maps3d.algorithm.index;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.math.Vector2D;

/**
 * Created by fdl on 8/8/16.
 */
public class BarycentricCoordinate { //TODO move it inside WrappedTriangle or smth and optimize object count
    final float weightB, weightC;

    private BarycentricCoordinate(float weightB, float weightC) {
        this.weightB = weightB;
        this.weightC = weightC;
    }

    /**
     * Factory method, producing barycentric coordinates by cartesian and triangle
     * {@see http://www.blackpawn.com/texts/pointinpoly/default.html}
     * @param p Coordinate of the point to place
     * @return two barycentric coordinates - weight of B and C
     */
    public static BarycentricCoordinate fromCartesian2D(Coordinate p, Geometry triangle) {
        assert Double.isNaN(p.z): "can compute coordinates only in two-dimensional space //" + p.z;
        assert triangle.getNumPoints() == 4: "can  compute barycentric coordinates only for triangle //" + triangle.getNumPoints();
        Coordinate[] coords = triangle.getCoordinates();
        Vector2D v0 = new Vector2D(coords[2].x - coords[0].x, coords[2].y - coords[0].y);
        Vector2D v1 = new Vector2D(coords[1].x - coords[0].x, coords[1].y - coords[0].y);
        Vector2D v2 = new Vector2D(p.x - coords[0].x, p.y - coords[0].y);

        // Compute dot products
        float dot00 = (float) v0.dot(v0);
        float dot01 = (float) v0.dot(v1);
        float dot02 = (float) v0.dot(v2);
        float dot11 = (float) v1.dot(v1);
        float dot12 = (float) v1.dot(v2);

        // Compute barycentric coordinates
        float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if point is in triangle
        return new BarycentricCoordinate(v, u);
    }

    public double getWeightedHeightIn3D(double heightA, double heightB, double heightC){
        return heightA*(1-weightB-weightC) + heightB*weightB + heightC*weightC;
    }
}
