package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.GeomUtils;
import Utils.Tracer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.math.Vector2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 27.07.2016.
 */
public class Interpolator {

    private IsolineContainer cont;
    private double step;
    private Envelope envelope;
    private Tracer<IIsoline> tracer;
    public Interpolator(IsolineContainer container, double interpolation_step) {
        this.cont = container;
        this.step = interpolation_step;
        this.envelope = container.getEnvelope();
        this.tracer = new Tracer<>(container,(x)->x.getLineString(),container.getFactory());
    }

    public double estimatePointHeight(Coordinate point, List<WeightedCoordinate> coords ) {
        double nearest_dist = 100000;
        double nearest_height = 0;
        for (WeightedCoordinate wc : coords) {
            if ( Math.abs(point.x-wc.x) < nearest_dist ) {
                if (Math.abs(point.y-wc.y) < nearest_dist) {
                    nearest_dist = (Math.abs(point.x-wc.x)+Math.abs(point.y-wc.y))*0.5;
                    nearest_height = wc.z;
                }
            }
        }
        return nearest_height;
    }



    public void writeDataToFile(String path) {

        InterpolatedContainer interp = new InterpolatedContainer(cont);

        double[][] heights = interp.getAllInterpolatingPoints();

        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }
        int y_steps = heights.length;
        int x_steps = heights[0].length;
//        int x_steps = (int)Math.ceil(envelope.getWidth()/step);
//        int y_steps = (int)Math.ceil(envelope.getHeight()/step);
//        double x_step = envelope.getWidth()/x_steps;
//        double y_step = envelope.getHeight()/y_steps;
//        Coordinate point = new Coordinate();

        System.out.println("Writing to file");
        for (int i = y_steps-1; i >= 0; --i) {
            //System.out.println( (y_steps-i) + " out of " + y_steps);
            //point.y = envelope.getMinY()+i*y_step;
            for (int j = 0; j != x_steps; ++j) {
                //point.x = envelope.getMinX()+j*x_step;
                //out.print(estimatePointHeight(point,coords)+" ");
                //if (heights[i][j] != null)
                out.print(heights[i][j]+" ");
                //else out.print("0 ");
            }
            out.println();
        }
        out.close();


        //getting bounds of possible height values
        double minHeight = heights[0][0], maxHeight = heights[0][0];
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                minHeight = Math.min(minHeight, heights[i][j]);
                maxHeight = Math.max(maxHeight, heights[i][j]);
            }
        }

        //creating visual heightmap
        BufferedImage image = new BufferedImage(x_steps, y_steps, BufferedImage.TYPE_INT_RGB);
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                int grey = (int) GeomUtils.map(heights[i][j], minHeight, maxHeight, 255, 0);
                image.setRGB(j, i, (((grey << 8) + (int)(grey*0.8)) << 8) + (int)(grey*0.5));
            }
        }

        //writing it to file
        try {
            File png = new File(path + ".png");
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new RuntimeException("Could not save " + path + ".png");
        }

    }

}
