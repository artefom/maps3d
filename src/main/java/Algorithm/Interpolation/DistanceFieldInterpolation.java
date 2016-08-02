package Algorithm.Interpolation;

import Utils.Constants;
import Utils.Intersector;
import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.PointRasterizer;
import com.vividsolutions.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs rasterisation and interpolation of isoline map.
 * Before doing this, make sure isolines have recovered height.
 *
 * This algorithm tries to copy the way human does estimation of point height between lines.
 *
 * It takes distance to 2 closest isolines and takes a weighted summ of their heights,
 * where wheights are distances to isolines.
 *
 * It uses distance field algorithm to calculate 2 closes isolines sitting on different heights for each pixel
 * of resulting image. Distance field algorithm implemented using 3x3 kernel as described here: https://habrahabr.ru/post/245729/
 */
public class DistanceFieldInterpolation {

    GeometryFactory gf;
    Isoline_attributed[] isolines;
    Intersector intersector;
    Envelope envelope;

    /**
     * Constructor
     * @param container container of isolines to be interpolated
     */
    public DistanceFieldInterpolation(IsolineContainer container) {
        envelope = container.getEnvelope();
        this.isolines = new Isoline_attributed[container.size()];
        List<Geometry> occluders = new ArrayList<>(container.size());
        int i = 0;
        for (IIsoline iso : container) {
            isolines[i] = new Isoline_attributed(iso);
            occluders.add(iso.getLineString());
            i+=1;
        }
        intersector = new Intersector(occluders,gf);
    }

    /**
     * Get min height of isolines in passed to constructor {@link IsolineContainer}
     * @return
     */
    public double getMinHeight() {
        double min_height = isolines[0].getIsoline().getHeight();
        for (int i = 1; i < isolines.length; ++i) {
            min_height = Math.min(min_height,isolines[i].getIsoline().getHeight());
        }
        return min_height;
    }

    /**
     * Get maximum of isoline heights in passed to constructor {@link IsolineContainer}
     * @return
     */
    public double getMaxHeight() {
        double max_height = isolines[0].getIsoline().getHeight();
        for (int i = 1; i < isolines.length; ++i) {
            max_height = Math.max(max_height,isolines[i].getIsoline().getHeight());
        }
        return max_height;
    }

    /**
     * Convert isoline height to height index.
     *
     * Height index should be integer.
     *
     * Isolines on different height should have different height index.
     * Isolines on same height should have same height index.
     */
    public void calculateHeightIndexes() {
        double min_height = getMinHeight();
        for (int i = 0; i != isolines.length; ++i) {
            isolines[i].setHeightIndex( (short) Math.round((isolines[i].getIsoline().getHeight()-min_height)*2) );
        }
    }


    /**
     * Perfomr a line rasterization algorithm (Paint {@link pixelInfo} buffer with necessery values, extraced from iso)
     * Following information about isoline is being rasterised to {@link pixelInfo} grid:
     *  Height index
     *  distance (equals 0, since pixel lies on isoline)
     * @param buf buffer to rasterize line to
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @param iso Isoline to take rasterization values from
     */
    public void rasterizeline(pixelInfo[][] buf, int x,int y,int x2, int y2, Isoline_attributed iso) {
        int w = x2 - x ;
        int h = y2 - y ;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
        if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
        if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
        if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
        int longest = Math.abs(w) ;
        int shortest = Math.abs(h) ;
        if (!(longest>shortest)) {
            longest = Math.abs(h) ;
            shortest = Math.abs(w) ;
            if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
            dx2 = 0 ;
        }
        int numerator = longest >> 1 ;
        for (int i=0;i<=longest;i++) {

            buf[x  ][y  ].height_index1 = iso.getHeightIndex();
            buf[x  ][y  ].distance1 = 0;

            numerator += shortest ;

            if (!(numerator<longest)) {
                numerator -= longest ;
                x += dx1 ;
                y += dy1 ;
            } else {
                x += dx2 ;
                y += dy2 ;
            }
        }
    }

    /**
     * Performs rasterization of line into int buffer
     *
     * int buffer is used as mask, where value of pixel is
     * -1 if it does not lie on isoline
     * height index of isoline, if it does lie on isoline
     * -2 if pixel lies on steep isoline (see {@link IIsoline#isSteep()})
     *
     * @param buf
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @param color
     */
    public void rasterizeline(int[][] buf, int x,int y,int x2, int y2, int color) {
        int w = x2 - x ;
        int h = y2 - y ;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
        if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
        if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
        if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
        int longest = Math.abs(w) ;
        int shortest = Math.abs(h) ;
        if (!(longest>shortest)) {
            longest = Math.abs(h) ;
            shortest = Math.abs(w) ;
            if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
            dx2 = 0 ;
        }
        int numerator = longest >> 1 ;
        for (int i=0;i<=longest;i++) {
            buf[x  ][y  ] = color;
            numerator += shortest ;
            if (!(numerator<longest)) {
                numerator -= longest ;
                x += dx1 ;
                y += dy1 ;
            } else {
                x += dx2 ;
                y += dy2 ;
            }
        }
    }

    /**
     * Rasterize isoline to buffer grid. see {@link DistanceFieldInterpolation#rasterizeline(pixelInfo[][], int, int, int, int, Isoline_attributed)}.
     */
    public void rasterize(pixelInfo[][] buf, Isoline_attributed line, PointRasterizer rasterizer, Isoline_attributed iso) {
        for (int coord_index = 1; coord_index < line.coordinates.length; ++coord_index) {

            int row1 = rasterizer.toRow(line.coordinates[coord_index-1].y);
            int col1 = rasterizer.toColumn(line.coordinates[coord_index-1].x);

            int row2 = rasterizer.toRow(line.coordinates[coord_index].y);
            int col2 = rasterizer.toColumn(line.coordinates[coord_index].x);

            if (row1 == 0) continue;
            if (col1 == 0) continue;
            if (row1+1 == rasterizer.getRowCount()) continue;
            if (col1+1 == rasterizer.getColumnCount()) continue;

            if (row2 == 0) continue;
            if (col2 == 0) continue;
            if (row2+1 == rasterizer.getRowCount()) continue;
            if (col2+1 == rasterizer.getColumnCount()) continue;

            rasterizeline(buf,row1,col1,row2,col2,iso);
        }
    }

    /**
     * Rasterize isoline to mask buffer grid.
     * When pixel of mask is not -1, distance can't propagate though it.
     * see {@link DistanceFieldInterpolation#rasterizeline(int[][], int, int, int, int, int)}
     */
    public void rasterize(int[][] buf, Isoline_attributed line, PointRasterizer rasterizer, int val) {
        for (int coord_index = 1; coord_index < line.coordinates.length; ++coord_index) {

            int row1 = rasterizer.toRow(line.coordinates[coord_index-1].y);
            int col1 = rasterizer.toColumn(line.coordinates[coord_index-1].x);

            int row2 = rasterizer.toRow(line.coordinates[coord_index].y);
            int col2 = rasterizer.toColumn(line.coordinates[coord_index].x);

            if (row1 == 0) continue;
            if (col1 == 0) continue;
            if (row1+1 == rasterizer.getRowCount()) continue;
            if (col1+1 == rasterizer.getColumnCount()) continue;

            if (row2 == 0) continue;
            if (col2 == 0) continue;
            if (row2+1 == rasterizer.getRowCount()) continue;
            if (col2+1 == rasterizer.getColumnCount()) continue;

            rasterizeline(buf,row1,col1,row2,col2,val);
        }
    }

    /**
     * Information about distance to some isoline and it's height
     *
     * Currently supposed to measure distances to 2 closes isolines
     *
     * Guaranteed: distance1 < distance2
     * Guaranteed: height_index1 != height_index2
     */
    private static class pixelInfo {
        public float distance1;
        public float distance2;
        public short height_index1;
        public short height_index2;

        public void validate() {

            // Swap if distance1 is less than distance2
            if (distance2 < distance1) {
                float dist_buf = distance1;
                short index_buf = height_index1;

                distance1 = distance2;
                height_index1 = height_index2;

                distance2 = dist_buf;
                height_index2 = index_buf;
            }

        }
    }

    /**
     * Put information about isoline into pixel, if distance to it is less than any of distances contained in pixel.
     * Ensure distance1 < distance2
     * Ensure height_index1 != height_index2
     */
    private void applyDistance(short height_index, float distance, pixelInfo info) {

        if (info.height_index1 == -1) {
            info.height_index1 = height_index;
            info.distance1 = distance;
            return;
        }

        if (info.height_index2 == -1) {
            info.height_index2 = height_index;
            info.distance2 = height_index;
            info.validate();
            return;
        }

        if (info.height_index1 == height_index) {
            info.distance1 = Math.min(distance,info.distance1);
            return;
        }

        if (info.height_index2 == height_index) {
            info.distance2 = Math.min(distance,info.distance2);
            info.validate();
            return;
        }

        if (distance < info.distance2) {
            info.distance2 = distance;
            info.height_index2 = height_index;
            info.validate();
            return;
        }

    }

    private float squareRootOfTwo = 1.41421356237309504880f;

    /**
     * Apply first pass distance-field kernel (see {@link DistanceFieldInterpolation})
     */
    private void applyDownPassKernel3x3(int row, int column, pixelInfo[][] buf, int[][] mask) {

        float current_distance = buf[row][column].distance1;
        short current_height_index = buf[row][column].height_index1;

        // Ensure we don't go though another isoline
        boolean canGoLeft=mask[row][column-1] == -1 || mask[row][column-1] == current_height_index;
        boolean canGoRight=mask[row][column+1] == -1 || mask[row][column+1] == current_height_index;
        boolean canGoDown=mask[row+1][column] == -1 || mask[row+1][column] == current_height_index;
        boolean canGoDownLeft = (canGoDown || canGoLeft) && mask[row+1][column-1] == -1 || mask[row+1][column-1] == current_height_index;
        boolean canGoDownRight = (canGoDown || canGoRight) && mask[row+1][column+1] == -1 || mask[row+1][column+1] == current_height_index;

        if (canGoRight)
            applyDistance(current_height_index,current_distance+1,buf[row][column+1]);
        if (canGoDown)
            applyDistance(current_height_index,current_distance+1,buf[row+1][column]);
        if (canGoDownRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row+1][column+1]);
        if (canGoDownLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row+1][column-1]);

        current_distance = buf[row][column].distance2;
        current_height_index = buf[row][column].height_index2;

        // Ensure we don't go though another isoline
        canGoLeft=mask[row][column-1] == -1 || mask[row][column-1] == current_height_index;
        canGoRight=mask[row][column+1] == -1 || mask[row][column+1] == current_height_index;
        canGoDown=mask[row+1][column] == -1 || mask[row+1][column] == current_height_index;
        canGoDownLeft = (canGoDown || canGoLeft) && (mask[row+1][column-1] == -1 || mask[row+1][column-1] == current_height_index);
        canGoDownRight = (canGoDown || canGoRight) && (mask[row+1][column+1] == -1 || mask[row+1][column+1] == current_height_index);

        if (canGoRight)
            applyDistance(current_height_index,current_distance+1,buf[row][column+1]);
        if (canGoDown)
            applyDistance(current_height_index,current_distance+1,buf[row+1][column]);
        if (canGoDownRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row+1][column+1]);
        if (canGoDownLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row+1][column-1]);


    }

    /**
     * Apply second pass distance-field kernel (see {@link DistanceFieldInterpolation})
     */
    private void applyUpPassKernel3x3(int row, int column, pixelInfo[][] buf, int[][] mask) {

        float current_distance = buf[row][column].distance1;
        short current_height_index = buf[row][column].height_index1;

        // Ensure we don't go though another isoline
        boolean canGoLeft=mask[row][column-1] == -1 || mask[row][column-1] == current_height_index;
        boolean canGoRight=mask[row][column+1] == -1 || mask[row][column+1] == current_height_index;
        boolean canGoUp=mask[row-1][column] == -1 || mask[row-1][column] == current_height_index;
        boolean canGoUpLeft= (canGoUp || canGoLeft) && (mask[row-1][column-1] == -1 || mask[row-1][column-1] == current_height_index);
        boolean canGoUpRight=(canGoUp || canGoRight) && (mask[row-1][column+1] == -1 || mask[row-1][column+1] == current_height_index);

        if (canGoLeft)
            applyDistance(current_height_index,current_distance+1,buf[row][column-1]);
        if (canGoUp)
            applyDistance(current_height_index,current_distance+1,buf[row-1][column]);
        if (canGoUpLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row-1][column-1]);
        if (canGoUpRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row-1][column+1]);

        current_distance = buf[row][column].distance2;
        current_height_index = buf[row][column].height_index2;

        // Ensure we don't go though another isoline
        canGoLeft=mask[row][column-1] == -1 || mask[row][column-1] == current_height_index;
        canGoRight=mask[row][column+1] == -1 || mask[row][column+1] == current_height_index;
        canGoUp=mask[row-1][column] == -1 || mask[row-1][column] == current_height_index;
        canGoUpLeft= (canGoUp || canGoLeft) && (mask[row-1][column-1] == -1 || mask[row-1][column-1] == current_height_index);
        canGoUpRight=(canGoUp || canGoRight) && (mask[row-1][column+1] == -1 || mask[row-1][column+1] == current_height_index);

        if (canGoLeft)
            applyDistance(current_height_index,current_distance+1,buf[row][column-1]);
        if (canGoUp)
            applyDistance(current_height_index,current_distance+1,buf[row-1][column]);
        if (canGoUpLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row-1][column-1]);
        if (canGoUpRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,buf[row-1][column+1]);
    }

    /**
     * Apply {@link DistanceFieldInterpolation#applyDownPassKernel3x3(int, int, pixelInfo[][], int[][])} to each pixel
     * @param source Rasterized information about isolines. Pivot pixels (with distance1 = 0) should be rasterized.
     * @param mask Mask with rasterized isoline heights. Height of isoline with height index x can't propagate through pixels of mask with value y, where x != y
     */
    private void DistanceCalculationDownPass(pixelInfo[][] source, int[][] mask) {
        int max_row = source.length-1; // Substract kernel size, so no buffer overflows
        int max_column = source[0].length-1;
        for (int row = 1; row != max_row; ++row) {
            for (int column = 1; column != max_column; ++column) {
                if (source[row][column].distance1 < max_distance)
                    applyDownPassKernel3x3(row,column,source,mask);
            }
        }
    }

    /**
     * Apply {@link DistanceFieldInterpolation#applyUpPassKernel3x3(int, int, pixelInfo[][], int[][])} to each pixel
     * @param source Rasterized information about isolines. Pivot pixels (with distance1 = 0) should be rasterized.
     * @param mask Mask with rasterized isoline heights. Height of isoline with height index x can't propagate through pixels of mask with value y, where x != y
     */
    private void DistanceCalculationUpPass(pixelInfo[][] source, int[][] mask) {
        int max_row = source.length-1; // Substract kernel size, so no buffer overflows
        int max_column = source[0].length-1;
        for (int row = max_row-1; row != 1; --row) {
            for (int column = max_column-1; column != 1; --column) {
                if (source[row][column].height_index1 != -1)
                    applyUpPassKernel3x3(row,column,source,mask);
            }
        }
    }


    /**
     * Applies up and down passes several times, so distance measurements can propagate around conners
     * (see{@link DistanceFieldInterpolation})
     * @param source Rasterized information about isolines. Pivot pixels (with distance1 = 0) should be rasterized.
     * @param mask Mask with rasterized isoline heights. Height of isoline with height index x can't propagate through pixels of mask with value y, where x != y
     */
    private void maskedDistanceField(pixelInfo[][] source, int[][] mask) {
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
    }

    /**
     * Applies up and down passes once. Used after performing {@link DistanceFieldInterpolation#maskedDistanceFieldSimple(pixelInfo[][], int[][])} to fill remaining gaps.
     * @param source Rasterized information about isolines. Pivot pixels (with distance1 = 0) should be rasterized.
     * @param mask Mask with rasterized isoline heights. Height of isoline with height index x can't propagate through pixels of mask with value y, where x != y
     */
    private void maskedDistanceFieldSimple(pixelInfo[][] source, int[][] mask) {
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
    }

    float max_distance = 100000000;

    /**
     * Retrive height map of isoline container, passed in {@link DistanceFieldInterpolation#DistanceFieldInterpolation(IsolineContainer)}.
     * @return height map. Heights are not normalized.
     */
    public double[][] getAllInterpolatingPoints() {

        calculateHeightIndexes();

        // Create cells
        PointRasterizer rasterizer = new PointRasterizer(Constants.INTERPOLATION_STEP,envelope);

        double pivot_height = Math.round(((getMaxHeight()+getMinHeight())))*0.5+0.5;
        pixelInfo[][] distanceField = new pixelInfo[rasterizer.getRowCount()][rasterizer.getColumnCount()];

        //Initialize pixel info matrix
        for (int row = 0; row != rasterizer.getRowCount(); ++row) {
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                distanceField[row][column] = new pixelInfo();
                distanceField[row][column].distance1 = max_distance;
                distanceField[row][column].distance2 = max_distance;
            }
        }

        int[][] mask = rasterizer.createIntBuffer(-1);

        //Populate cells
        for (int isoline_id = 0; isoline_id != isolines.length; ++isoline_id) {
            Isoline_attributed iso = isolines[isoline_id];

            if (!iso.getIsoline().isSteep()) {
                rasterize(distanceField, iso, rasterizer, iso);
                rasterize(mask, iso, rasterizer, iso.getHeightIndex());
            }
        }

        for (int isoline_id = 0; isoline_id != isolines.length; ++isoline_id) {
            Isoline_attributed iso = isolines[isoline_id];
            if (iso.getIsoline().isSteep())
                rasterize(mask, iso, rasterizer, -2);
        }

        maskedDistanceField(distanceField,mask);

        mask = rasterizer.createIntBuffer(-2);

        for(int row = 0; row != rasterizer.getRowCount(); ++row) {
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                if (distanceField[row][column].height_index1 == -1 || distanceField[row][column].distance1 == max_distance) {
                    mask[row][column] = -1;
                }
            }
        }

        for (int isoline_id = 0; isoline_id != isolines.length; ++isoline_id) {
            Isoline_attributed iso = isolines[isoline_id];
            if (iso.getIsoline().isSteep())
                rasterize(mask, iso, rasterizer, -1);
        }

        maskedDistanceFieldSimple(distanceField,mask);

        // Calculate distance
        double[][] result = new double[rasterizer.getRowCount()][rasterizer.getColumnCount()];
        for(int row = 0; row != rasterizer.getRowCount(); ++row) {
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {

                if (distanceField[row][column].height_index1 == -1) {
                    result[row][column] = 0;
                    continue;
                }


                double dist1 = distanceField[row][column].distance1*Constants.DRAWING_INTERPOLATION_STEP;
                double dist2 = distanceField[row][column].distance2*Constants.DRAWING_INTERPOLATION_STEP;
                // Calculate distance fade
                if (dist1 > Constants.INTERPOLATION_FADE_DISTANCE) {
                    dist1 = Constants.INTERPOLATION_FADE_DISTANCE+Math.pow(dist1-Constants.INTERPOLATION_FADE_DISTANCE,Constants.INTERPOLATION_FADE_STRENGTH);
                }
                if (dist2 > Constants.INTERPOLATION_FADE_DISTANCE) {
                    dist2 = Constants.INTERPOLATION_FADE_DISTANCE+Math.pow(dist2-Constants.INTERPOLATION_FADE_DISTANCE,Constants.INTERPOLATION_FADE_STRENGTH);
                }
                double w1 = dist2/(dist1+dist2);
                double w2 = dist1/(dist1+dist2);
                result[row][column] = distanceField[row][column].height_index1*w1+distanceField[row][column].height_index2*w2;
            }
        }

        // gaussian blur
        double[][] gaussian_buf = new double[rasterizer.getRowCount()][rasterizer.getColumnCount()];

        for (int i = 0; i != 2; ++i) {
            for (int row = 0; row != rasterizer.getRowCount(); ++row) {
                for (int column = 1; column != rasterizer.getColumnCount() - 1; ++column) {
                    gaussian_buf[row][column] = (result[row][column - 1]+result[row][column]+result[row][column + 1])/3;
                }
                for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                    result[row][column] = gaussian_buf[row][column];
                }
            }

            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                for (int row = 1; row != rasterizer.getRowCount() - 1; ++row) {
                    gaussian_buf[row][column] = (result[row-1][column]+result[row][column]+result[row+1][column])/3;
                }
                for (int row = 0; row != rasterizer.getRowCount(); ++row) {
                    result[row][column] = gaussian_buf[row][column];
                }
            }
        }

        //return
        return result;
    }

}
