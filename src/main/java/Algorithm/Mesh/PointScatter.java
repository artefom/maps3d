package Algorithm.Mesh;

import Utils.*;
import Utils.Area.PointAreaBuffer;
import com.vividsolutions.jts.geom.Coordinate;

import java.awt.image.Raster;
import java.util.*;

/**
 * Created by Artyom.Fomenko on 25.08.2016.
 */
public class PointScatter {

    @FunctionalInterface
    public interface AreaAssessor {
        double assess(double x0,double y0, double x1, double y1);
    }

    Random r = new Random();

    int width = 0;
    int height = 0;

    int maxPointCount = 3000000;
    double maxDensity = maxPointCount;///((x_max-x_min)*(y_max-y_min));

    double y_min;
    double y_max;

    double x_min;
    double x_max;

    int[] sorted_indexes;
    float[] dist;

    ArrayList<Pair<Integer,Float>> sorted;

    float d_min;
    float d_max;

    double minRadius;
    double maxRadius = 0;
    double radiusCutoff = 0;

    double minHeight = 0;
    double maxHeight = 0;

    double sobelCutoff = 0.001;

    //NodedFunction nf;

    public PointScatter() {
    }

    public void setDistribution(float[] distribution, int width, int height) {

        this.width = width;
        this.height = height;

        dist = distribution;

        sorted = new ArrayList<>();
        sorted.add(new Pair<>(0,distribution[0]));
        d_min = distribution[0];
        d_max = distribution[0];
        for (int i = 1; i < distribution.length; ++i) {
            d_min = Math.min(distribution[i],d_min);
            d_max = Math.max(distribution[i],d_max);
            sorted.add(new Pair<>(i,distribution[i]));
        }
        sorted.sort((lhs,rhs)->Float.compare(rhs.v2,lhs.v2));
    }

    public int getMaxPointCount() {
        return maxPointCount;
    }

    public void setMaxPointCount(int maxPointCount) {
        this.maxPointCount = maxPointCount;
        maxDensity = maxPointCount/((x_max-x_min)*(y_max-y_min));
    }

    public void setEnvelope(double x0, double y0, double x1, double y1) {
        y_min = y0;
        y_max = y1;

        x_min = x0;
        x_max = x1;

        maxDensity = maxPointCount/((x_max-x_min)*(y_max-y_min));
    }

    public double getMinRadius() {
        return minRadius;
    }

    public void setMinRadius(double minRadius) {
        this.minRadius = minRadius;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
    }


    public double getRadiusCutoff() {
        return radiusCutoff;
    }

    public void setRadiusCutoff(double radiusCutoff) {
        this.radiusCutoff = radiusCutoff;
    }

    public double getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(double minHeight) {
        this.minHeight = minHeight;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public double getSobelCutoff() {
        return sobelCutoff;
    }

    public void setSobelCutoff(double sobelCutoff) {
        this.sobelCutoff = sobelCutoff;
    }

    public void getNext(Coordinate c) {
        get(r.nextDouble(),c);
    }

    public Coordinate getNext() {
        return get(r.nextDouble());
    }

    public Coordinate get( double seed ) {
        Coordinate buf = new Coordinate();
        get(seed,buf);
        return buf;
    }

    private void get(double seed, Coordinate c) {
        int pos = (int)(GeomUtils.map( seed, 0, 1, 0, dist.length ));
        c.x = GeomUtils.map(pos%width,0,width,x_min,x_max);
        c.y = GeomUtils.map(pos/width,0,height,y_min,y_max);
    }

    public void getWeightedSumm(double c_x, double c_y, double r, Coordinate ret) {
        int center_row = (int)(GeomUtils.map(c_y,y_min,y_max,0,height));
        int center_column = (int)(GeomUtils.map(c_x,x_min,x_max,0,width));
        int row_r = (int)(GeomUtils.map(r,y_min,y_max,0,height));
        int column_r = (int)(GeomUtils.map(r,x_min,x_max,0,width));

        int begin_x =   (int)GeomUtils.clamp(center_column-column_r,0,width-1);
        int end_x   =   (int)GeomUtils.clamp(center_column+column_r,0,width-1);
        int begin_y =   (int)GeomUtils.clamp(center_row-row_r,0,height-1);
        int end_y   =   (int)GeomUtils.clamp(center_row+row_r,0,height-1);

        double x_sum = 0;
        double y_sum = 0;
        double weight_summ = 0;

        for (int x = begin_x; x <= end_x; ++x) {
            for (int y = begin_y; y <= end_y; ++y) {
                double w = dist[y*width+x];
                x_sum += x*w;
                y_sum += y*w;
                weight_summ += w;
            }
        }

        ret.x = GeomUtils.map(x_sum/weight_summ,0,width-1,x_min,x_max);
        ret.y = GeomUtils.map(y_sum/weight_summ,0,height-1,y_min,y_max);

    }

    public double getRadius(double value) {
        double density = GeomUtils.map(value,d_min,d_max,0,maxDensity);
        //if (density < 0.001) break;
        double area = 2.0/density;
        double r = Math.sqrt(area);
        if (r>maxRadius) r = maxRadius;
        return r;
    }

    public PointAreaBuffer buffer;
    public void scatterPoints(Collection<Coordinate> out) {

        if (sorted == null) throw new RuntimeException("Distribution not set!");
        if (d_min == d_max) throw new RuntimeException("Distribution bounds not set!");
        if (x_max == x_min || y_max == y_min) throw  new RuntimeException("Envelope not set!");

        buffer = new PointAreaBuffer();
        buffer.setEnvelope(x_min,x_max,y_min,y_max);
        buffer.init(200,200);

        CommandLineUtils.reportProgressBegin("Scattering points");

        int counter = 0;
        for (Pair<Integer,Float> dto : sorted) {

            CommandLineUtils.reportProgress(++counter,sorted.size());
            int row = dto.v1/width;
            int column = dto.v1%width;
            double x = GeomUtils.map(column,0,width,x_min,x_max);
            double y = GeomUtils.map(row,0,height,y_min,y_max);
            double r = getRadius(dto.v2);
            if (!buffer.hasInRadius(x,y,r)) {
                Coordinate c = new Coordinate(x,y);

                buffer.add(c);
                out.add(c);

            }


        }

        CommandLineUtils.reportProgressEnd();
    }

    public static void main(String[] args) {
        System.out.println("Hello, world");

    }

}
