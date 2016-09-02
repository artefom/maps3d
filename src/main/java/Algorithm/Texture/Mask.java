package Algorithm.Texture;


import Algorithm.Mesh.TexturedPatch;
import Utils.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;

import java.awt.*;
import java.awt.image.*;
import java.util.List;

/**
 * Created by Artyom.Fomenko on 12.08.2016.
 */
public class Mask {

    private BufferedImage image;
    private static final double gamma = 2.2;
    int width;
    int height;
    byte[] pixels;
    float[] opacity;

    public Mask(int width, int height) {
        image = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);

        WritableRaster raster = image.getRaster();
        this.width = raster.getWidth();
        this.height = raster.getHeight();
        this.pixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        this.opacity = new float[this.pixels.length];
        fill((byte)-128);
    }


    /**
     * Flush whole scene with color
     * @param color color to flush with
     */
    public void fill(byte color) {
        for (int i = 0; i != pixels.length; ++i) pixels[i] = color;
    }


    public void fillPolygons(List<Polygon> polygons, PointRasterizer rasterizer, byte color) {
        short[][] mask = rasterizer.createShortBuffer();

        for (Polygon p : polygons) {
            rasterizer.fillPrepass(mask,p);
        }

        for (int y = 0; y != image.getHeight(); ++y) {
            int counter = 0;
            for (int x = 0; x != image.getWidth(); ++x) {
                if (mask[y][x] > 0)
                    counter+=mask[y][x];
                if (counter != 0) pixels[width*(height-1-y)+x] = color;
                if (mask[y][x] < 0)
                    counter+=mask[y][x];
            }
        }
    }

    public void drawLines(List<LineString> strings, PointRasterizer rasterizer, byte color, int width) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setClip(0,0,image.getWidth(),image.getHeight());
        g.setColor(new Color(color,color,color,255));
        g.setStroke(new BasicStroke(width));
        int height = image.getHeight();
        for (LineString s : strings) {
            LineSegment buf = new LineSegment();
            LineStringIterator it = new LineStringIterator(s,buf);
            while (it.hasNext()) {
                it.next();

                if (rasterizer.isInBounds(buf.p0.x,buf.p0.y) || rasterizer.isInBounds(buf.p1.x,buf.p1.y)) {
                    g.drawLine(rasterizer.toColumn(buf.p0.x), height - 1 - rasterizer.toRow(buf.p0.y),
                            rasterizer.toColumn(buf.p1.x), height - 1 - rasterizer.toRow(buf.p1.y));
                }
            }
        }
    }

    public void clean() {
        for (int i = 0; i != pixels.length; ++i) {
            pixels[i] = -128;
        }
    }


    public Graphics2D getGraphics() {
        return (Graphics2D)image.getGraphics();
    }
//
//    public void blur() {
//        RasterUtils.gauss(pixels,width,height,5,3);
//    }

    public enum BlendMode { NONE, OVERLAY, MULTIPLY, SCREEN }

    public void overlay(BufferedImage img, int color, BlendMode blendMode) {

        int[] img_pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        short dr = getR(color);
        short dg = getG(color);
        short db = getB(color);
        short da = getA(color);

        for (int i = 0; i != img_pixels.length; ++i) {
            int scolor = img_pixels[i];

            short sr = getR(scolor);
            short sg = getG(scolor);
            short sb = getB(scolor);
            short sa = getA(scolor);

            if (blendMode != BlendMode.NONE) {
                dr = getR(color);
                dg = getG(color);
                db = getB(color);
                da = getA(color);
            }

            float a = ((float)(pixels[i] + 128) / 255)*((float)da/255);
            float a2 = 1 - a;


            if (blendMode == BlendMode.SCREEN) {
                dr = (short)(255-(float)(255-dr)*(255-sr)/255);
                dg = (short)(255-(float)(255-dg)*(255-sg)/255);
                db = (short)(255-(float)(255-db)*(255-sb)/255);
            } else if (blendMode == BlendMode.MULTIPLY) {
                dr = (short)((float)dr*sr/255);
                dg = (short)((float)dg*sr/255);
                db = (short)((float)db*sr/255);
            } else if (blendMode == BlendMode.OVERLAY) {
                dr = sr < 128 ? (short)(2.0*dr*sr/255) : (short)(255-2.0*(255-dr)*(255-sr)/255);
                dg = sg < 128 ? (short)(2.0*dg*sg/255) : (short)(255-2.0*(255-dg)*(255-sg)/255);
                db = sb < 128 ? (short)(2.0*db*sb/255) : (short)(255-2.0*(255-db)*(255-sb)/255);
            }

            sr = (short) (getR(scolor) * a2 + dr * a);
            sg = (short) (getG(scolor) * a2 + dg * a);
            sb = (short) (getB(scolor) * a2 + db * a);
            sa = (short) (255 - ((255 - sa) * a2));
            img_pixels[i] = toRGB(sr, sg, sb, sa);
        }
    }

    //byte[] mask_pixels = null;
    /**
     * Draw image over tex, using this mask
     * @param img
     * @param tex
     * @param blendMode
     */
    public void overlay(BufferedImage img, BufferedImage tex, BlendMode blendMode, Envelope texture_envelope ) {

        int[] img_pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        int img_width = img.getWidth();
        int img_height = img.getHeight();
        int tex_width = tex.getWidth();
        int tex_height = tex.getHeight();

        byte[] mask_pixels = new byte[img_width * img_height];
            TextureUtils.drawOver(mask_pixels,img_width,img_height,pixels,width,height,new Envelope(
                    new Coordinate(0,0),
                    new Coordinate(img_width-1,img_height-1)
            ),false);

        TextureUtils.drawOver(img_pixels,img_width,img_height,tex,mask_pixels,texture_envelope,true,blendMode);
    }

    /* Atomic functions, working with bits */

    public static int toRGB(short r, short g, short b, short a) {
        return (a << 24) | (r << 16) | (g << 8) | (b);
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
