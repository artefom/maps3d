package Algorithm.Texture;


import Utils.GeomUtils;
import Utils.LineStringIterator;
import Utils.OutputUtils;
import Utils.PointRasterizer;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import sun.dc.pr.Rasterizer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 12.08.2016.
 */
public class Texture {

    private BufferedImage image;
    private static final double gamma = 2.2;

    public Texture(int width, int height) {
        image = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
    }

    public void setRGB(int x, int y, short r, short g, short b) {
        setRGBA(x,y,r,g,b,getA(x,y));
    }

    public void setRGB(int x, int y, int rgb) {
        image.setRGB(x,y,rgb);
    }

    public int getRGB(int x, int y) {
        return image.getRGB(x,y);
    }

    /**
     * Fill texture by mask with color
     * @param mask short matrix. matrix row count must match texture hegiht and column count - texure width
     * @param color
     */
    public void fill(float[][] mask, int color) {
        int height = mask.length;
        int width = mask[0].length;

        /*
        short fill_r = applyGamma( getR(color), 1.0/gamma);
        short fill_g = applyGamma( getG(color), 1.0/gamma);
        short fill_b = applyGamma( getB(color), 1.0/gamma);
        short fill_a = applyGamma( getA(color), 1.0/gamma);
        */

        short fill_r = getR(color);
        short fill_g = getG(color);
        short fill_b = getB(color);
        short fill_a = getA(color);
        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                double w2 = mask[row][column]*((double)fill_a/255);
                double w1 = 1-w2;

                short pivot_r = getR(column,height-row-1);
                short pivot_g = getG(column,height-row-1);
                short pivot_b = getB(column,height-row-1);

                short r = GeomUtils.clamp( (short)(pivot_r*w1+fill_r*w2), (short)0, (short)255);
                short g = GeomUtils.clamp( (short)(pivot_g*w1+fill_g*w2), (short)0, (short)255);
                short b = GeomUtils.clamp( (short)(pivot_b*w1+fill_b*w2), (short)0, (short)255);
                short a = GeomUtils.clamp( (short)(w1*255+w2*255), (short)0, (short)255);
                setRGBA(column,height-row-1,r,g,b,a);

            }
        }
    }

//    public void applyGamma(double gamma) {
//        for (int y = 0; y != image.getHeight(); ++y) {
//            for (int x = 0; x != image.getWidth(); ++x) {
//                image.setRGB(x,y,applyRGBGamma(image.getRGB(x,y),gamma));
//            }
//        }
//    }

    /**
     * Flush whole scene with color
     * @param color color to flush with
     */
    public void fill(int color) {
        Graphics g = image.getGraphics();
        g.setColor(new Color(color));
        g.fillRect(0,0,image.getWidth(),image.getHeight());
    }

    public void fillPolygons(List<Polygon> polygons, PointRasterizer rasterizer, int color) {
        short[][] mask = rasterizer.createShortBuffer();
        for (Polygon p : polygons) {
            rasterizer.fillPrepass(mask,p);
        }
        int height = image.getHeight();
        for (int y = 0; y != image.getHeight(); ++y) {
            int counter = 0;
            for (int x = 0; x != image.getWidth(); ++x) {
                if (mask[y][x] > 0)
                    counter+=mask[y][x];
                if (counter != 0) setRGB(x,height-y-1,color);
                if (mask[y][x] < 0)
                    counter+=mask[y][x];
            }
        }
    }

    public void drawLines(List<LineString> strings, PointRasterizer rasterizer, int color, int width) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setColor(new Color(color));
        g.setStroke(new BasicStroke(width));
        int height = image.getHeight();
        for (LineString s : strings) {
            LineSegment buf = new LineSegment();
            LineStringIterator it = new LineStringIterator(s,buf);
            while (it.hasNext()) {
                it.next();
                g.drawLine( rasterizer.toColumn(buf.p0.x), height-1-rasterizer.toRow(buf.p0.y),
                        rasterizer.toColumn(buf.p1.x), height-1-rasterizer.toRow(buf.p1.y));
            }
        }
    }

    public void save(String path) {

        String extenstion = OutputUtils.getExtension(path);
        try {
            File f = new File(path);
//            applyGamma(1/gamma);
            ImageIO.write(image, extenstion, f);
        } catch (IOException e) {
//            applyGamma(gamma);
            throw new RuntimeException("Could not save " + path);
        }

    }


//    public short applyGamma(short color, double gamma) {
//        return (short)(255 * Math.pow((double)color/255,gamma));
//    }
//
//    public int applyRGBGamma(int color, double gamma) {
//        short r = applyGamma( getR(color), gamma);
//        short g = applyGamma( getG(color), gamma);
//        short b = applyGamma( getB(color), gamma);
//        short a = applyGamma( getA(color), gamma);
//        return toRGB(r,g,b,a);
//    }

    /* Atomic functions, working with bits */
    public void setRGBA(int x, int y, short r, short g, short b, short a) {
        image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | (b) );
    }

    public static int toRGB(short r, short g, short b, short a) {
        return (a << 24) | (r << 16) | (g << 8) | (b);
    }

    public short getR(int x, int y) {
        return (short)((image.getRGB(x,y) >> 16) & 0xff);
    }

    public short getG(int x, int y) {
        return (short)((image.getRGB(x,y) >> 8 ) & 0xff);
    }

    public short getB(int x, int y) {
        return (short)((image.getRGB(x, y)     ) & 0xff);
    }

    public short getA(int x, int y) {
        return (short)((image.getRGB(x,y) >> 24) & 0xff);
    }

    public void setR(int x, int y, short r) {
        image.setRGB(x,y,  (image.getRGB(x,y) | (0xff << 16)) & (r << 16) );
    }

    public void setG(int x, int y, short g) {
        image.setRGB(x,y,  (image.getRGB(x,y) | (0xff << 8)) & (g << 8) );
    }

    public void setB(int x, int y, short b) {
        image.setRGB(x,y,  (image.getRGB(x,y) | (0xff << 0)) & (b << 0) );
    }

    public void setA(int x, int y, short a) {
        image.setRGB(x,y,  (image.getRGB(x,y) | (255 << 24)) & (a << 24) );
    }

    public static short getR(int color) {
        return (short)((color >> 16) & 0xff);
    }

    public static short getG(int color) {
        return (short)((color >> 8 ) & 0xff);
    }

    public static short getB(int color) {
        return (short)((color     ) & 0xff);
    }

    public static short getA(int color) {
        return (short)((color >> 24) & 0xff);
    }

    public static int setR(int color, short r) {
        return (color | (0xff << 16)) & (r << 16);
    }

    public static int setG(int color, short g) {
        return (color | (0xff << 8)) & (g << 8);
    }

    public static int setB(int color, short b) {
        return (color | (0xff << 0)) & (b << 0);
    }

    public static int setA(int color, short a) {
        return  (color | (255 << 24)) & (a << 24);
    }
    /* No more work with bits */

}
