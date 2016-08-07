package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.GeomUtils;
import Utils.RasterUtils;
import Utils.Tracer;
import com.vividsolutions.jts.geom.Envelope;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Serializes map, calculated by {@link DistanceFieldInterpolation} to various file types
 */
public class Serializer {

    private IsolineContainer cont;
    private double step;
    private Envelope envelope;
    private Tracer<IIsoline> tracer;
    public Serializer(IsolineContainer container, double interpolation_step) {
        this.cont = container;
        this.step = interpolation_step;
        this.envelope = container.getEnvelope();
        this.tracer = new Tracer<>(container,(x)->x.getLineString(),container.getFactory());
    }




    public void writeDataToFile(String path) {

        DistanceFieldInterpolation interp = new DistanceFieldInterpolation(cont);

        double[][] heights = interp.getAllInterpolatingPoints();

        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }
        int y_steps = heights.length;
        int x_steps = heights[0].length;

        System.out.println("Writing to file");
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                out.print(heights[i][j]+" ");
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
                int grey = 255-(int)GeomUtils.map(heights[i][j], minHeight, maxHeight, 255, 0);
                image.setRGB(j, y_steps-i-1, (((grey << 8) + (int)(grey)) << 8) + (int)(grey));
            }
        }

        //writing it to file
        try {
            File png = new File(path + ".png");
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new RuntimeException("Could not save " + path + ".png");
        }

        //Saving .obj file
        Triangulation tri = new Triangulation(heights, RasterUtils.sobel( RasterUtils.sobel(heights) ));
        tri.writeToFile(path);

    }

    public void saveAsObj(String path) {

        DistanceFieldInterpolation interp = new DistanceFieldInterpolation(cont);
        double[][] heightmap = interp.getAllInterpolatingPoints();

        Triangulation tri = new Triangulation(heightmap, RasterUtils.sobel( RasterUtils.sobel(heightmap) ));
        tri.writeToFile(path);

    }

}
