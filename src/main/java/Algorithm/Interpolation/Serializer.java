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

        RasterUtils.saveAsTxt(heights,path);
        RasterUtils.saveAsPng(heights,path);

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
