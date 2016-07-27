package Algorithm.Interpolation;

import Isolines.IIsoline;
import Isolines.IsolineContainer;
import Utils.GeomUtils;
import Utils.Tracer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.math.Vector2D;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

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

    public double estimatePointHeight(Coordinate point) {
        int angle_steps = 8;
        double angle_step = (Math.PI*2)/(angle_steps);
        Vector2D vec = Vector2D.create(0,1);
        double hit_num = 0;
        double height_accum = 0;
        double weight_accum = 0;
        for (int i = 0; i < angle_steps; ++i) {
            Tracer<IIsoline>.traceres res = tracer.trace(point, vec,0,100);
            vec = vec.rotate(angle_step);
            if (res.entitiy != null && !res.entitiy.isSteep()) {
                hit_num+=1;
                if (res.distance < 0.01) return res.entitiy.getHeight();
                double weight = 1.0/res.distance;
                weight_accum+=weight;
                height_accum+=(res.entitiy.getHeight()*weight);
            }
        }
        if (hit_num < 2) {
            return -100000;
        } else {
            return height_accum/weight_accum;
        }
    }

    public void writeDataToFile(String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(path);
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save output");
        }
        int x_steps = (int)Math.ceil(envelope.getWidth()/step);
        int y_steps = (int)Math.ceil(envelope.getHeight()/step);
        double x_step = envelope.getWidth()/x_steps;
        double y_step = envelope.getHeight()/y_steps;
        Coordinate point = new Coordinate();
        for (int i = y_steps; i >= 0; --i) {
            point.y = envelope.getMinY()+i*y_step;
            for (int j = 0; j <= x_steps; ++j) {
                point.x = envelope.getMinX()+j*x_step;
                out.print(estimatePointHeight(point)+" ");
            }
            out.println();
        }
        out.close();
    }

}
