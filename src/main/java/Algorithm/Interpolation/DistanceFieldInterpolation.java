package Algorithm.Interpolation;

import Utils.Intersector;
import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.PointRasterizer;
import com.vividsolutions.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class DistanceFieldInterpolation {

    GeometryFactory gf;
    Isoline_attributed[] isolines;
    Intersector intersector;
    Envelope envelope;

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

    public double getMinHeight() {
        double min_height = isolines[0].getIsoline().getHeight();
        for (int i = 1; i < isolines.length; ++i) {
            min_height = Math.min(min_height,isolines[i].getIsoline().getHeight());
        }
        return min_height;
    }

    public double getMaxHeight() {
        double max_height = isolines[0].getIsoline().getHeight();
        for (int i = 1; i < isolines.length; ++i) {
            max_height = Math.max(max_height,isolines[i].getIsoline().getHeight());
        }
        return max_height;
    }

    public void calculateHeightIndexes() {
        double min_height = getMinHeight();
        for (int i = 0; i != isolines.length; ++i) {
            isolines[i].setHeightIndex( (short) Math.round((isolines[i].getIsoline().getHeight()-min_height)*2) );
        }
    }


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
    private void applyDownPassKernel3x3(int row, int column, pixelInfo[][] buf, int[][] mask) {

        float current_distance = buf[row][column].distance1;
        short current_height_index = buf[row][column].height_index1;

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

    private void applyUpPassKernel3x3(int row, int column, pixelInfo[][] buf, int[][] mask) {

        float current_distance = buf[row][column].distance1;
        short current_height_index = buf[row][column].height_index1;

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


    private void maskedDistanceField(pixelInfo[][] source, int[][] mask) {
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
    }

    private void maskedDistanceFieldSimple(pixelInfo[][] source, int[][] mask) {
        DistanceCalculationDownPass(source,mask);
        DistanceCalculationUpPass(source,mask);
    }

    float max_distance = 100000000;
    public double[][] getAllInterpolatingPoints() {

        calculateHeightIndexes();

        // Create cells
        PointRasterizer rasterizer = new PointRasterizer(0.25,envelope);

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

        double[][] result = new double[rasterizer.getRowCount()][rasterizer.getColumnCount()];
        for(int row = 0; row != rasterizer.getRowCount(); ++row) {
            for (int column = 0; column != rasterizer.getColumnCount(); ++column) {
                double dist1 = distanceField[row][column].distance1;
                double dist2 = distanceField[row][column].distance2;
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
        return gaussian_buf;
    }

}
