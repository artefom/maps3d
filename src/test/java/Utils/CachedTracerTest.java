package Utils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Artyom.Fomenko on 22.08.2016.
 */
public class CachedTracerTest {

    private static class TestEntity{

        public Geometry g;

        public int id = 0;

        public TestEntity() {
        }

        public TestEntity(int id, Geometry g) {
            this.id = id;
            this.g = g;
        }

        @Override
        public String toString() {
            return Integer.toString(id);
        }
    }

    Random r = new Random( 42 );
    private double getNextDouble(double min, double max) {
        return GeomUtils.map(r.nextDouble(),0,1,min,max);
    }

    @Test
    @Ignore
    public void testCachedTracerRandom() {

        for (int test_iteration = 0; test_iteration != 100; ++test_iteration) {
//            System.out.println("Testing cached tracer");
//
//            System.out.println("Generating 2-point strings");

            double min_x = 0;
            double max_x = 100;
            double min_y = 0;
            double max_y = 100;

            double d_x = 10;
            double d_y = 10;

            GeometryFactory gf = new GeometryFactory();
            ArrayList<TestEntity> entities = new ArrayList<>();

            double x_step = (max_x - min_x) / 100;
            for (int i = 0; i != 5000; ++i) {

                double x = min_x + i * x_step;
                double y1 = min_y;
                double y2 = max_y;

//            Coordinate p0 = new Coordinate( x , y1 );
//            Coordinate p1 = new Coordinate( x , y2 );

                Coordinate p0 = new Coordinate(getNextDouble(min_x, max_x), getNextDouble(min_y, max_y));
                Coordinate p1 = new Coordinate(p0.x + getNextDouble(-d_x, d_x), p0.y + getNextDouble(-d_y, d_y));
                LineString ls = gf.createLineString(new Coordinate[]{p0, p1});
                TestEntity ent = new TestEntity(i, ls);
                entities.add(ent);
            }

            Tracer_Legacy<TestEntity> tracer_legacy = new Tracer_Legacy<>(entities, (x) -> x.g, gf);
            CachedTracer<TestEntity> tracer = new CachedTracer<>(entities, (x) -> x.g, gf);


            ArrayList<LineString> base = new ArrayList<>();
            for (TestEntity ent : entities) {
                base.add((LineString) ent.g);
            }

            ArrayList<LineSegment> legacy = new ArrayList<>();
            ArrayList<LineSegment> tested = new ArrayList<>();

            long timeA = System.currentTimeMillis();

            for (int i = 0; i != 1000; ++i) {

                Coordinate p0 = new Coordinate(getNextDouble(min_x, max_x), getNextDouble(min_y, max_y));
                Coordinate p1 = new Coordinate(p0.x + getNextDouble(-d_x, d_x), p0.y + getNextDouble(-d_y, d_y));

                Vector2D vec = Vector2D.create(p0, p1);
                Tracer_Legacy.traceres tr_legacy = tracer_legacy.trace(p0, vec, 0, 1);
                CachedTracer.traceres tr = tracer.trace(p0, vec, 0, 1);

                if (Math.abs(tr.distance - tr_legacy.distance) > 0.001 ||
                        tr.side != tr_legacy.side ||
                        tr.entitiy != tr_legacy.entitiy
                        ) {
                    System.out.println("Trace result do not match");

                    System.out.println("Expected: " + tr_legacy.toString());
                    System.out.println("Got: " + tr.toString());

                }

                //legacy.add(new LineSegment(p0, CoordUtils.add(vec.multiply(tr_legacy.distance).toCoordinate(), p0)));
                //tested.add(new LineSegment(p0, CoordUtils.add(vec.multiply(tr.distance).toCoordinate(), p0)));

//            System.out.println("Legacy hit distance: " + Math.round(tr_legacy.distance/vec.length()*1000)/10+"%");
//            System.out.println("Hit distance: " + Math.round(tr.distance/vec.length()*1000)/10+"%" );
            }
            long timeB = System.currentTimeMillis();
            System.out.println("Elapsed time: " + (timeB - timeA));

//            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("test.txt"), "utf-8"))) {
//                for (LineString ls : base) {
//                    writer.print("base: ");
//                    for (int i = 0; i != ls.getCoordinateSequence().size(); ++i) {
//                        writer.print(ls.getCoordinateSequence().getX(i) + " " + ls.getCoordinateSequence().getY(i) + " ");
//                    }
//                    writer.println();
//                }
//
//                for (LineSegment ls : legacy) {
//                    writer.println("legacy: " + ls.p0.x + " " + ls.p0.y + " " + ls.p1.x + " " + ls.p1.y);
//                }
//
//                for (LineSegment ls : tested) {
//                    writer.println("test: " + ls.p0.x + " " + ls.p0.y + " " + ls.p1.x + " " + ls.p1.y);
//                }
//
//
//                writer.println("env: " + tracer.buffer.envelope_minX + " " + tracer.buffer.envelope_maxX + " " +
//                        tracer.buffer.envelope_minY + " " + tracer.buffer.envelope_maxY);
//                for (int i = 0; i < tracer.buffer.height; ++i) {
//                    writer.print("buf: ");
//                    for (int j = 0; j != tracer.buffer.width; ++j) {
//                        writer.print(tracer.buffer.getCell(j, i).size());
//                        if (j != tracer.buffer.width - 1) writer.print(" ");
//                    }
//                    writer.println();
//                }
//            } catch (Exception ignored) {
//
//            }
        }

    }

}