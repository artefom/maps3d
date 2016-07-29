package Algorithm.Interpolation;

import Algorithm.LineConnection.Intersector;
import Isolines.IIsoline;
import Utils.DTW;
import Utils.GeomUtils;
import Utils.LineStringInterpolatedPointIterator;
import Utils.Tracer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class Isoline_attributed {

    public Coordinate[] coordinates;
    private IIsoline isoline;
    private double[] distances_positive;
    private Isoline_attributed[] isolines_positive;
    private int[] indexes_positive;

    private double[] distances_negative;
    private Isoline_attributed[] isolines_negative;
    private int[] indexes_negative;

    private short heightIndex = -1;


    public Isoline_attributed(IIsoline isoline) {
        this.isoline = isoline;
        ArrayList<Coordinate> coordinates_list = new ArrayList<>();
        LineStringInterpolatedPointIterator it = new LineStringInterpolatedPointIterator(isoline.getLineString(),0.25,0);
        while (it.hasNext()) {
            coordinates_list.add(it.next());
        }
        coordinates = coordinates_list.toArray(new Coordinate[coordinates_list.size()]);
        distances_positive = new double[coordinates.length];
        isolines_positive = new Isoline_attributed[coordinates.length];
        indexes_positive = new int[coordinates.length];

        distances_negative = new double[coordinates.length];
        isolines_negative = new Isoline_attributed[coordinates.length];
        indexes_negative = new int[coordinates.length];
    }

    private void matchIfLessInternal(Isoline_attributed other,
                                     double[] distances,
                                     Isoline_attributed[] isolines,
                                     int[] indexes, int side, Intersector intersector) {
        double self_height = getIsoline().getHeight();
        double other_height = other.getIsoline().getHeight();
        double height_diff = Math.abs(self_height-other_height);
        if ( height_diff > 1.1 ) return;
        Coordinate[] selfCoords = coordinates;
        Coordinate[] otherCoords = other.coordinates;
        Coordinate p1;
        Coordinate p2;
        Coordinate p3;
        Coordinate ppivot;

        double max_dist = 20;
        for (int i = 1; i < distances.length-1; ++i) {
            Coordinate pivot = selfCoords[i];
            for (int j = 1; j < otherCoords.length-1; ++j) {
                Coordinate dest = otherCoords[j];
                double dist =  pivot.distance(dest);
                if (dist > max_dist || dist == 0) continue;
                // Check for line side
                p1 = selfCoords[i-1];
                p2 = selfCoords[i];
                p3 = selfCoords[i+1];

                // Alter distance by angle

                //double dest_angle = ;
                Vector2D v1 = new Vector2D(p1,p3).normalize();
                Vector2D v2 = new Vector2D(pivot,dest).normalize();
                Vector2D v3 = new Vector2D(otherCoords[j-1],otherCoords[j+1]).normalize();
                double source_angle = Math.abs(v1.dot(v2));
                double dest_angle = Math.abs(v3.dot(v2));
                double angle_mean = source_angle+dest_angle;
                double dist_normalized = dist/max_dist;
                double dist_score = 1-(dist_normalized*dist_normalized);
                double angle_score = 1-(angle_mean*angle_mean);
                double beta = 0.01;

                double score = (beta+1)*(dist_score*angle_score)/(beta*dist_score+angle_score);

                if (height_diff < 0.1) score = score*0.5;

                if ( (distances[i] < score || isolines[i] == null) && source_angle < 0.5 && dest_angle < 0.5 ) {

                    if (getSide(dest,p1,p2,p3) != side) continue;

                    // Check for self-intersection
                    if (Tracer.intersects(this.getIsoline().getLineString(),new LineSegment(pivot,dest),0.1,0.9))
                        continue;

                    if (intersector.intersects(new LineSegment(pivot,dest))) continue;

                    distances[i] = score;
                    isolines[i] = other;
                    indexes[i] = j;
                }
            }
        }

    }


    public void matchIfLess(Isoline_attributed other, Intersector intersector) {
        matchIfLessInternal(other,distances_positive,isolines_positive,indexes_positive,1, intersector);
        matchIfLessInternal(other,distances_negative,isolines_negative,indexes_negative,-1, intersector);
    }

    public void RemoveDuplicates() {
        Set<Isoline_attributed> connected_isolines = new HashSet<>();
        for (int i = 0; i != isolines_negative.length; ++i) connected_isolines.add(isolines_negative[i]);
        for (int i = 0; i != isolines_positive.length; ++i) connected_isolines.add(isolines_positive[i]);
        for (Isoline_attributed other : connected_isolines) {
            if (other == null) continue;
            // Remove duplicates
            for (int i = 0; i != other.isolines_negative.length; ++i) {

                if (other.isolines_positive[i] != null && other.isolines_positive[i].getIsoline() == this.getIsoline()) {
                    int this_index = other.indexes_positive[i];
                    if (isolines_positive[this_index] == other && indexes_positive[this_index] == i) {
                        isolines_positive[this_index] = null;}
                    if (isolines_negative[this_index] == other && indexes_negative[this_index] == i) {
                        isolines_negative[this_index] = null;}
                }

                if (other.isolines_negative[i] != null && other.isolines_negative[i].getIsoline() == this.getIsoline()) {
                    int this_index = other.indexes_negative[i];
                    if (isolines_positive[this_index] == other && indexes_positive[this_index] == i) {
                        isolines_positive[this_index] = null;}
                    if (isolines_negative[this_index] == other && indexes_negative[this_index] == i) {
                        isolines_negative[this_index] = null;}
                }
            }
        }
    }

    public IIsoline getIsoline() {
        return isoline;
    }

    private List<LineSegment> getMatchingLinesInternal(GeometryFactory gf,
                                                      Isoline_attributed[] isolines,
                                                      int[] indexes) {
        Coordinate[] selfCoords = this.coordinates;
        ArrayList<LineSegment> ret = new ArrayList<>();
        for (int i = 0; i != isolines.length; ++i) {
            if (isolines[i] == null) continue;
            Coordinate pivot = selfCoords[i];
            Coordinate target = isolines[i].coordinates[indexes[i]];
            pivot.z = this.getIsoline().getHeight();
            target.z = isolines[i].getIsoline().getHeight();
            ret.add(new LineSegment(pivot,target));
        }
        return ret;
    }

    /**
     * Used for displaying connections
     */
    public List<LineSegment> getMatchingLines(GeometryFactory gf) {
        List<LineSegment> strings1 = getMatchingLinesInternal(gf,isolines_positive,indexes_positive);
        List<LineSegment> strings2 = getMatchingLinesInternal(gf,isolines_negative,indexes_negative);
        strings1.addAll(strings2);
        return strings1;
    }

    public static int getSide(Coordinate pivot, Coordinate p1, Coordinate p2, Coordinate p3) {
        LineSegment ls1 = new LineSegment(p1,p2);
        LineSegment ls2 = new LineSegment(p2,p3);
        if (ls1.projectionFactor(pivot) < 1) return GeomUtils.getSide(ls1,pivot);
        return GeomUtils.getSide(ls2,pivot);
    }

    public short getHeightIndex() {
        return heightIndex;
    }

    public void setHeightIndex(short heightIndex) {
        this.heightIndex = heightIndex;
    }
}
