package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.*;
import Utils.Properties.PropertiesLoader;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.math.Vector2D;

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
    CachedTracer<Geometry> intersector;
    Envelope envelope;

    /**
     * Constructor
     * @param container container of isolines to be interpolated
     */
    public DistanceFieldInterpolation(IsolineContainer container) {
        envelope = container.getEnvelope();
        PropertiesLoader.update();
        rasterizer = new PointRasterizer(PropertiesLoader.getInterpolationStep(),envelope);
        this.isolines = new Isoline_attributed[container.size()];
        List<Geometry> occluders = new ArrayList<>(container.size());
        int i = 0;
        for (IIsoline iso : container) {
            isolines[i] = new Isoline_attributed(iso);
            occluders.add(iso.getLineString());
            i+=1;
        }
        intersector = new CachedTracer<>(occluders,(x)->x,gf);
    }

    PointRasterizer rasterizer;

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

    public PointRasterizer getRasterizer() {
        return rasterizer;
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
    public void rasterizeline(pixelInfo[][] buf, int x,int y,int x2, int y2, Isoline_attributed iso, Vector2D dir) {
        float dir_x = (float)dir.getX();
        float dir_y = (float)dir.getY();
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
            buf[x  ][y  ].pivot1_column = (short)y;
            buf[x  ][y  ].pivot1_row = (short)x;
            buf[x  ][y  ].dirX = dir_x;
            buf[x  ][y  ].dirY = dir_y;
            buf[x  ][y  ].slopeSide = (byte) iso.getIsoline().getSlopeSide();

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

    /*
                buf[row1  ][column1  ].height_index1 = iso.getHeightIndex();
            buf[row1  ][column1  ].distance1 = 0;
            buf[row1  ][column1  ].pivot1_column = (short)column1;
            buf[row1  ][column1  ].pivot1_row = (short)row1;
            buf[row1  ][column1  ].dirX = dir_x;
            buf[row1  ][column1  ].dirY = dir_y;
            buf[row1  ][column1  ].slopeSide = (byte) iso.getIsoline().getSlopeSide();
     */


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
     * Rasterize isoline to buffer grid. see {@link DistanceFieldInterpolation#rasterizeline(pixelInfo[][], int, int, int, int, Isoline_attributed,Vector2D)}.
     */
    public void rasterize(pixelInfo[][] buf, Isoline_attributed line, PointRasterizer rasterizer, Isoline_attributed iso) {
        for (int coord_index = 1; coord_index < line.coordinates.size(); ++coord_index) {

            double x1 = line.coordinates.getX(coord_index-1);
            double y1 = line.coordinates.getY(coord_index-1);
            double x2 = line.coordinates.getX(coord_index);
            double y2 = line.coordinates.getY(coord_index);

            int row1 = rasterizer.toRowClamped(y1);
            int col1 = rasterizer.toColumnClamped(x1);

            int row2 = rasterizer.toRowClamped(y2);
            int col2 = rasterizer.toColumnClamped(x2);

            if (row1 == 0) continue;
            if (col1 == 0) continue;
            if (row1+1 == rasterizer.getRowCount()) continue;
            if (col1+1 == rasterizer.getColumnCount()) continue;

            if (row2 == 0) continue;
            if (col2 == 0) continue;
            if (row2+1 == rasterizer.getRowCount()) continue;
            if (col2+1 == rasterizer.getColumnCount()) continue;

            Vector2D dir = Vector2D.create(y2-y1,x2-x1).normalize();

            rasterizeline(buf,row1,col1,row2,col2,iso,dir);
        }
    }

    /**
     * Rasterize isoline to mask buffer grid.
     * When pixel of mask is not -1, distance can't propagate though it.
     * see {@link DistanceFieldInterpolation#rasterizeline(int[][], int, int, int, int, int)}
     */
    public void rasterize(int[][] buf, Isoline_attributed line, PointRasterizer rasterizer, int val) {
        for (int coord_index = 1; coord_index < line.coordinates.size(); ++coord_index) {

            int row1 = rasterizer.toRowClamped(line.coordinates.getY(coord_index-1));
            int col1 = rasterizer.toColumnClamped(line.coordinates.getX(coord_index-1));

            int row2 = rasterizer.toRowClamped(line.coordinates.getY(coord_index));
            int col2 = rasterizer.toColumnClamped(line.coordinates.getX(coord_index));

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


        // Row and column of closest pixel
        public short pivot1_column;
        public short pivot1_row;

        // Row and column of closest pixel
        public short pivot2_column;
        public short pivot2_row;


        //Direction (used only on isoline pixels. can be optimized)
        public float dirX;
        public float dirY;
        //Slope side (used only on isoline pixels. can be optimized)
        public byte slopeSide;

        //Tangent of slope angle (used only on isoline pixels. can be optimized)
        public float tangent_cache;


        pixelInfo() {
            distance1 = Constants.INTERPOLATION_MAX_DISTANCE;
            distance2 = Constants.INTERPOLATION_MAX_DISTANCE;
            height_index1 = -1;
            height_index2 = -1;
            dirX = 0;
            dirY = 0;
            slopeSide = 0;
        }

        public void sort() {

            // Swap if distance1 is less than distance2
            if (distance2 < distance1) {
                float dist_buf = distance1;
                short index_buf = height_index1;
                short pivot_column_buf = pivot1_column;
                short pivot_row_buf = pivot1_row;

                distance1 = distance2;
                distance2 = dist_buf;

                height_index1 = height_index2;
                height_index2 = index_buf;

                pivot1_column = pivot2_column;
                pivot2_column = pivot_column_buf;

                pivot1_row = pivot2_row;
                pivot2_row = pivot_row_buf;

            }

        }
    }

    /**
     * Put information about isoline into pixel, if distance to it is less than any of distances contained in pixel.
     * Ensure distance1 < distance2
     * Ensure height_index1 != height_index2
     */
    private void applyDistance(short height_index, float distance, short pivot_column, short pivot_row, pixelInfo info) {

        // init old values to ensure info.height_index1 != info.height_index2
        if (info.height_index1 == height_index) {
            if (info.distance1 > distance) {
                info.distance1 = distance;
                info.pivot1_column = pivot_column;
                info.pivot1_row = pivot_row;
            }
            return;
        }

        // init old values to ensure info.height_index1 != info.height_index2
        if (info.height_index2 == height_index) {
            if (info.distance2 > distance) {
                info.distance2 = distance;
                info.pivot2_column = pivot_column;
                info.pivot2_row = pivot_row;
                info.sort();
            }
            return;
        }

        // finally, if distance is less than info's distance2 put new value to height_index2 and sort values
        if (distance < info.distance2) {
            info.distance2 = distance;
            info.height_index2 = height_index;
            info.pivot2_column = pivot_column;
            info.pivot2_row = pivot_row;

            // Sort to ensure distance1 < distance2
            info.sort();
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
        short pivot_column = buf[row][column].pivot1_column;
        short pivot_row = buf[row][column].pivot1_row;

        if (canGoRight)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row][column+1]);
        if (canGoDown)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row+1][column]);
        if (canGoDownRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row+1][column+1]);
        if (canGoDownLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row+1][column-1]);

        /* Do the second time for second values */
        current_distance = buf[row][column].distance2;
        current_height_index = buf[row][column].height_index2;

        // Ensure we don't go though another isoline
        canGoLeft=mask[row][column-1] == -1 || mask[row][column-1] == current_height_index;
        canGoRight=mask[row][column+1] == -1 || mask[row][column+1] == current_height_index;
        canGoDown=mask[row+1][column] == -1 || mask[row+1][column] == current_height_index;
        canGoDownLeft = (canGoDown || canGoLeft) && (mask[row+1][column-1] == -1 || mask[row+1][column-1] == current_height_index);
        canGoDownRight = (canGoDown || canGoRight) && (mask[row+1][column+1] == -1 || mask[row+1][column+1] == current_height_index);
        pivot_column = buf[row][column].pivot2_column;
        pivot_row = buf[row][column].pivot2_row;

        if (canGoRight)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row][column+1]);
        if (canGoDown)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row+1][column]);
        if (canGoDownRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row+1][column+1]);
        if (canGoDownLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row+1][column-1]);


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
        short pivot_column = buf[row][column].pivot1_column;
        short pivot_row = buf[row][column].pivot1_row;

        if (canGoLeft)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row][column-1]);
        if (canGoUp)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row-1][column]);
        if (canGoUpLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row-1][column-1]);
        if (canGoUpRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row-1][column+1]);

        current_distance = buf[row][column].distance2;
        current_height_index = buf[row][column].height_index2;

        // Ensure we don't go though another isoline
        canGoLeft=mask[row][column-1] == -1 || mask[row][column-1] == current_height_index;
        canGoRight=mask[row][column+1] == -1 || mask[row][column+1] == current_height_index;
        canGoUp=mask[row-1][column] == -1 || mask[row-1][column] == current_height_index;
        canGoUpLeft= (canGoUp || canGoLeft) && (mask[row-1][column-1] == -1 || mask[row-1][column-1] == current_height_index);
        canGoUpRight=(canGoUp || canGoRight) && (mask[row-1][column+1] == -1 || mask[row-1][column+1] == current_height_index);
        pivot_column = buf[row][column].pivot2_column;
        pivot_row = buf[row][column].pivot2_row;

        if (canGoLeft)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row][column-1]);
        if (canGoUp)
            applyDistance(current_height_index,current_distance+1,pivot_column,pivot_row,buf[row-1][column]);
        if (canGoUpLeft)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row-1][column-1]);
        if (canGoUpRight)
            applyDistance(current_height_index,current_distance+squareRootOfTwo,pivot_column,pivot_row,buf[row-1][column+1]);
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
                if (source[row][column].distance1 < Constants.INTERPOLATION_MAX_DISTANCE)
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

        CommandLineUtils.reportProgressBegin("Calculating distance field");
        for (int i = 0; i != 3; ++i) {
            CommandLineUtils.reportProgress(i*2,6);
            DistanceCalculationDownPass(source,mask);
            CommandLineUtils.reportProgress(i*2+1,6);
            DistanceCalculationUpPass(source,mask);
        }
        CommandLineUtils.reportProgressEnd();
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


    /**
     * Used to calculate smooth transition between isolines, taking in account slope angle and make hills.
     * Based on bezier curves.
     * @param t1 first slope angle (positive - hill)
     * @param t2 second slope angle (positive - hill)
     * @param h1 height of first isoline
     * @param h2 height of second isoline
     * @param d1 distance from first isoline
     * @param d2 distance from second isoline
     * @return height of point between isolines
     */
    public double heightCalc(double t1,double t2,double h1,double h2,double d1,double d2) {
        t2 = -t2;

        double x1 = 0;
        double y1 = h1;

        double x2 = d1+d2;
        double y2 = h2;

        double strength = 0.5;

        double b1x = 1;
        double b1y = t1*(b1x);
        double b1_len = Math.sqrt(b1x*b1x+b1y*b1y);
        b1x=b1x/b1_len*strength;
        b1y=b1y/b1_len*strength;

        double b2x = -1;
        double b2y = t2*(b2x);
        double b2_len = Math.sqrt(b2x*b2x+b2y*b2y);
        b2x=b2x/b2_len*strength;
        b2y=b2y/b2_len*strength;

        b1x = b1x+0;
        b1y = b1y+h1;

        b2x = b2x+d1+d2;
        b2y = b2y+h2;

        //Do binary search

        double pos_beg = 0;
        double pos_end = 1;
        double pos = (pos_beg+pos_end)*0.5;
        double pos2 = 1-pos;

        double x = ((x1*pos2+b1x*pos)*pos2+(b1x*pos2+b2x*pos)*pos)*pos2+
                ((b1x*pos2+b2x*pos)*pos2+(b2x*pos2+x2*pos)*pos)*pos;

        for (int i = 0; i != 15; ++i) {
            if (x > d1) {
                pos_end = pos;
            } else {
                pos_beg = pos;
            }
            pos = (pos_beg+pos_end)*0.5;
            pos2 = 1-pos;
            x = ((x1*pos2+b1x*pos)*pos2+(b1x*pos2+b2x*pos)*pos)*pos2+
                    ((b1x*pos2+b2x*pos)*pos2+(b2x*pos2+x2*pos)*pos)*pos;
        }
        double y = ((y1*pos2+b1y*pos)*pos2+(b1y*pos2+b2y*pos)*pos)*pos2+
                ((b1y*pos2+b2y*pos)*pos2+(b2y*pos2+y2*pos)*pos)*pos;
        return y;
    }

    /**
     * Used to determine, weather the given point is higher or lower than it's closest isoline.
     * Based on cross product.
     * Negative values - given point is lower
     * Positive values - given point is higher
     * When a perpendicular can be drawn to closest isoline, absolute return value is between 0.85-1.0
     * Usie absolute value for calculating smooth transitions between downside or upside of isoline.
     * @param buf
     * @param row
     * @param column
     * @return cross product of vector(row-pivot_row,column-pivot_column) and vector(buf[pivot_row][pivot_column].dirX,buf[pivot_row][pivot_column].dirY)
     * multiplied by isoline's slope side.
     * where pivot_row is uf[row][column].pivot1_row and pivot_column is buf[row][column].pivot1_column.
     */
    public double crossProductSideDetect(pixelInfo[][] buf, int row, int column) {

        int pivot_row = buf[row][column].pivot1_row;
        int pivot_column = buf[row][column].pivot1_column;
        Vector2D vec = Vector2D.create(buf[pivot_row][pivot_column].dirX,buf[pivot_row][pivot_column].dirY);

        double v1x = buf[pivot_row][pivot_column].dirX;
        double v1y = buf[pivot_row][pivot_column].dirY;
        double v2x = row-pivot_row;
        double v2y = column-pivot_column;
        double v2Len = Math.sqrt(v2x*v2x+v2y*v2y);
        v2x = v2x/v2Len;
        v2y = v2y/v2Len;
        return (v1x*v2y - v1y*v2x)*buf[pivot_row][pivot_column].slopeSide;

    }

    double calcRealDistance(double pixelDist) {
        double result = pixelDist*PropertiesLoader.getInterpolationStep()/10;
        return result;
    }

    double calcRealDistanceWithFade(double pixelDist) {
        pixelDist/=3;
        double result = pixelDist*PropertiesLoader.getInterpolationStep()/10;
        if (result > PropertiesLoader.interpolation.fade_distance) {
            result = PropertiesLoader.interpolation.fade_distance+Math.pow(result-PropertiesLoader.interpolation.fade_distance,
                    PropertiesLoader.interpolation.fade_strength);
        }
        return result;
    }

    /**
     * Retrive height map of isoline container, passed in {@link DistanceFieldInterpolation#DistanceFieldInterpolation(IsolineContainer)}.
     * @return height map. Heights are not normalized.
     */
    public double[][] getAllInterpolatingPoints() {

        calculateHeightIndexes();

        pixelInfo[][] distanceField = new pixelInfo[rasterizer.getRowCount()][rasterizer.getColumnCount()];

        //Initialize pixel info matrix
        for (int row = 0; row != rasterizer.getRowCount(); ++row) {
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                distanceField[row][column] = new pixelInfo();
            }
        }

        int[][] mask = rasterizer.createIntBuffer(-1);

        CommandLineUtils.reportProgressBegin("Rasterizing polygons");
        //Populate cells
        int count = 0;
        for (int isoline_id = 0; isoline_id != isolines.length; ++isoline_id) {
            CommandLineUtils.reportProgress(++count,isolines.length*2);
            Isoline_attributed iso = isolines[isoline_id];

            if (!iso.getIsoline().isSteep()) {
                rasterize(distanceField, iso, rasterizer, iso);
                rasterize(mask, iso, rasterizer, iso.getHeightIndex());
            }
        }

        for (int isoline_id = 0; isoline_id != isolines.length; ++isoline_id) {
            CommandLineUtils.reportProgress(++count,isolines.length*2);
            Isoline_attributed iso = isolines[isoline_id];
            if (iso.getIsoline().isSteep())
                rasterize(mask, iso, rasterizer, -2);
        }
        CommandLineUtils.reportProgressEnd();


        // Calculate distance field
        maskedDistanceField(distanceField,mask);

        // Fill gaps
        mask = rasterizer.createIntBuffer(-2);

        CommandLineUtils.reportProgressBegin("Filling gaps");
        for(int row = 0; row != rasterizer.getRowCount(); ++row) {
            CommandLineUtils.reportProgress(row,rasterizer.getRowCount());
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                if (distanceField[row][column].height_index1 == -1 || distanceField[row][column].distance1 == Constants.INTERPOLATION_MAX_DISTANCE) {
                    mask[row][column] = -1;
                }
            }
        }
        CommandLineUtils.reportProgressEnd();

        for (int isoline_id = 0; isoline_id != isolines.length; ++isoline_id) {
            Isoline_attributed iso = isolines[isoline_id];
            if (iso.getIsoline().isSteep())
                rasterize(mask, iso, rasterizer, -1);
        }

        maskedDistanceFieldSimple(distanceField,mask);

        // Calculate height for each pixel

        CommandLineUtils.reportProgressBegin("Calculating heights");

        double[][] result = new double[rasterizer.getRowCount()][rasterizer.getColumnCount()];
        for(int row = 0; row != rasterizer.getRowCount(); ++row) {
            CommandLineUtils.reportProgress(row,rasterizer.getRowCount());
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                pixelInfo p = distanceField[row][column];
                if (p.height_index1 == -1) {
                    result[row][column] = 0;
                    continue;
                }
                double dist1 = calcRealDistanceWithFade(p.distance1);
                double dist2 = calcRealDistanceWithFade(p.distance2);

                double w1 = dist2 / (dist1 + dist2);
                double w2 = dist1 / (dist1 + dist2);
                double h1 = p.height_index1 * w1 + p.height_index2 * w2;

                result[row][column] = h1;
            }
        }
        CommandLineUtils.reportProgressEnd();

        //Calculate heights for hills
        //gauss(result,1,2);
        double[][] sobel = RasterUtils.sobel(result);


        CommandLineUtils.reportProgressBegin("Calculating hill heights");

        for(int row = 0; row != rasterizer.getRowCount(); ++row) {
            CommandLineUtils.reportProgress(row,rasterizer.getRowCount());
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                pixelInfo p = distanceField[row][column];
                double dist1 = calcRealDistance(p.distance1);
                double dist2 = calcRealDistance(p.distance2);

                // Calculate hills
                if (dist2>PropertiesLoader.interpolation.fade_distance && p.distance1 != 0) {
                    //result[row][column] = 0;
                    double h2 = 0;
                    // Use tangent of slope of closest isoline. Retrieve it from previously calculated sobel
                    double tangent = sobel[p.pivot1_row][p.pivot1_column]*2;

                    // When tangent is almost 0, but we want a hill to be visible on the map. Assign high value to tangent, so it will
                    // We could use conditional expression, like tangent = tangent < 0.2 ? 5 : tangent, but it's better to use
                    // this function for calulation of smooth transition.
                    // This function is only significant at low tangent values ( < 0.2 ) and otherwise almost equals tangent
                    tangent = (1-1/(1+Math.exp(-tangent*40+2)))*3+tangent;

                    //tangent = tangent/Constants.INTERPOLATION_STEP;
                    // Just make sure everything goes fine and no extreme values are encountered.
                    tangent = GeomUtils.clamp(tangent,0.05,5.7);

                    double ss = crossProductSideDetect(distanceField, row, column);
                    h2 = 1 - 1.0 / (tangent * p.distance1*PropertiesLoader.getInterpolationStep()/10 + 1);
                    if (ss <= 0) h2 = -h2;
                    h2 = GeomUtils.clamp(h2, -1, 1);
                    h2 = h2 * GeomUtils.clamp(GeomUtils.map(dist2, PropertiesLoader.interpolation.fade_distance, PropertiesLoader.interpolation.fade_distance * 2, 0, 1), 0, 1);
                    h2 = GeomUtils.clamp(h2, -1, 1);
                    result[row][column] += h2;
                }

            }
        }

        CommandLineUtils.reportProgressEnd();

        int[][] gaussian_blur_mask = rasterizer.createIntBuffer(1);

        for (int row = 0; row != rasterizer.getRowCount(); ++row) {
            for (int column = 1; column != rasterizer.getColumnCount() - 1; ++column) {
                gaussian_blur_mask[row][column] = (int)Math.floor(distanceField[row][column].distance1*0.3);
                //if (gaussian_blur_mask[row][column] < 1) gaussian_blur_mask[row][column] = 1;
                //if (gaussian_blur_mask[row][column] > 50) gaussian_blur_mask[row][column] = 50;
            }
        }

        CommandLineUtils.reportProgressBegin("Applying gaussian blur");
        RasterUtils.gauss(result,1,1);
        CommandLineUtils.reportProgress(1,2);
        RasterUtils.gauss(result,gaussian_blur_mask,3);
        CommandLineUtils.reportProgressEnd();

        //return
        return result;
    }

}
