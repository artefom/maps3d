package Utils;

import Algorithm.Texture.Mask;
import com.vividsolutions.jts.geom.Envelope;

import java.awt.image.BufferedImage;

/**
 * Created by Artyom.Fomenko on 01.09.2016.
 */
public class TextureUtils {

    public static int getRGB(BufferedImage image, int x, int y, boolean tile, int empty_color) {

        if (tile) {
                x = x % image.getWidth();
            if (x < 0)
                x += image.getWidth();

            y = y % image.getHeight();
            if (y < 0)
                y += image.getHeight();
        } else {
            if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
                return empty_color;
            }
        }

        return image.getRGB(x,y);
    }

    public static byte getGray(byte[] image, int image_width, int image_height, int x, int y, boolean tile, byte empty_color) {

        if (tile) {
            x = x % image_width;
            if (x < 0)
                x += image_width;

            y = y % image_height;
            if (y < 0)
                y += image_height;
        } else {
            if (x < 0 || y < 0 || x >= image_width || y >= image_height) {
                return empty_color;
            }
        }

        return image[y*image_width+x];
    }

    public static int bilinearInterpolation(double x, double y, int col_00, int col_10, int col_01, int col_11) {
        double area_11 = (x)*(y);
        double area_01 = (1-x)*(y);
        double area_10 = (x)*(1-y);
        double area_00 = (1-x)*(1-y);

        double d_r = (double)RasterUtils.getR(col_00)*area_00+
                (double)RasterUtils.getR(col_10)*area_10+
                (double)RasterUtils.getR(col_01)*area_01+
                (double)RasterUtils.getR(col_11)*area_11;
        double d_g = (double)RasterUtils.getG(col_00)*area_00+
                (double)RasterUtils.getG(col_10)*area_10+
                (double)RasterUtils.getG(col_01)*area_01+
                (double)RasterUtils.getG(col_11)*area_11;
        double d_b = (double)RasterUtils.getB(col_00)*area_00+
                (double)RasterUtils.getB(col_10)*area_10+
                (double)RasterUtils.getB(col_01)*area_01+
                (double)RasterUtils.getB(col_11)*area_11;

        int r = GeomUtils.clamp((int)Math.round(d_r),0,255);
        int g = GeomUtils.clamp((int)Math.round(d_g),0,255);
        int b = GeomUtils.clamp((int)Math.round(d_b),0,255);

        return RasterUtils.asRGB(r,g,b,255);
    }

    public static byte bilinearInterpolation(double x, double y, byte col_00, byte col_10, byte col_01, byte col_11) {
        double area_11 = (x)*(y);
        double area_01 = (1-x)*(y);
        double area_10 = (x)*(1-y);
        double area_00 = (1-x)*(1-y);

        double d_g = (double)col_00*area_00+(double)col_10*area_10+(double)col_01*area_01+(double)col_11*area_11;

        return (byte)GeomUtils.clamp(d_g+0.5,-128,127);
    }

    public static int interpolate(Envelope image_envelope, BufferedImage image, double x, double y, boolean tile) {
        double image_x = GeomUtils.map(x,image_envelope.getMinX(),image_envelope.getMaxX(),-0.5,image.getWidth()-0.5);
        double image_y = GeomUtils.map(y,image_envelope.getMinY(),image_envelope.getMaxY(),-0.5,image.getHeight()-0.5);

        int min_x = (int)Math.floor(image_x);
        int min_y = (int)Math.floor(image_y);
        int max_y = min_y+1;
        int max_x = min_x+1;

        int col_00 = getRGB(image,min_x,min_y,tile, 0);
        int col_10 = getRGB(image,max_x,min_y,tile, 0);
        int col_01 = getRGB(image,min_x,max_y,tile, 0);
        int col_11 = getRGB(image,max_x,max_y,tile, 0);

        return bilinearInterpolation(image_x-min_x,image_y-min_y,col_00,col_10,col_01,col_11);
    }

    public static byte interpolate(Envelope image_envelope, byte[] image, int image_width, int image_height, double x, double y, boolean tile) {
        double image_x = GeomUtils.map(x,image_envelope.getMinX(),image_envelope.getMaxX(),0,image_width);
        double image_y = GeomUtils.map(y,image_envelope.getMinY(),image_envelope.getMaxY(),0,image_height);

        int min_x = (int)Math.floor(image_x);
        int min_y = (int)Math.floor(image_y);
        int max_y = min_y+1;
        int max_x = min_x+1;

        byte col_00 = getGray(image,image_width,image_height,min_x,min_y,tile, (byte)-128);
        byte col_10 = getGray(image,image_width,image_height,max_x,min_y,tile, (byte)-128);
        byte col_01 = getGray(image,image_width,image_height,min_x,max_y,tile, (byte)-128);
        byte col_11 = getGray(image,image_width,image_height,max_x,max_y,tile, (byte)-128);

        return bilinearInterpolation(image_x-min_x,image_y-min_y,col_00,col_10,col_01,col_11);
    }

    public static void drawOver( BufferedImage image, BufferedImage tex, Envelope envelope, boolean tile) {
        for (int column = 0; column != image.getWidth(); ++column) {
            for (int row = 0; row != image.getHeight(); ++row) {

                double r_accum = 0;
                double g_accum = 0;
                double b_accum = 0;
                int count = 0;

                for (int interp_x = 0; interp_x != 4; ++interp_x) {
                    for (int interp_y = 0; interp_y != 4; ++interp_y) {
                        double x = column+0.25*interp_x+0.125;
                        double y = row+0.25*interp_y+0.125;
                        int c = interpolate(envelope,tex,x,y,tile);
                        r_accum += RasterUtils.getR(c);
                        g_accum += RasterUtils.getG(c);
                        b_accum += RasterUtils.getB(c);
                        count += 1;
                    }
                }

                if (count != 0) {
                    r_accum /= count;
                    g_accum /= count;
                    b_accum /= count;
                }

                int r = GeomUtils.clamp( (int)Math.round(r_accum), 0, 255);
                int g = GeomUtils.clamp( (int)Math.round(g_accum), 0, 255);
                int b = GeomUtils.clamp( (int)Math.round(b_accum), 0, 255);

                image.setRGB(column,row,RasterUtils.asRGB(r,g,b));
            }
        }
    }

    public static void drawOver( int[] image, int width, int height, BufferedImage tex, byte[] mask, Envelope envelope, boolean tile, Mask.BlendMode blendMode) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {


                double r_accum = 0;
                double g_accum = 0;
                double b_accum = 0;
                int count = 0;

                for (int interp_x = 0; interp_x != 4; ++interp_x) {
                    for (int interp_y = 0; interp_y != 4; ++interp_y) {
                        double x = column+0.25*interp_x+0.125;
                        double y = row+0.25*interp_y+0.125;
                        int c = interpolate(envelope,tex,x,y,tile);
                        r_accum += RasterUtils.getR(c);
                        g_accum += RasterUtils.getG(c);
                        b_accum += RasterUtils.getB(c);
                        count += 1;
                    }
                }

                if (count != 0) {
                    r_accum /= count;
                    g_accum /= count;
                    b_accum /= count;
                }

                short dr = (short)GeomUtils.clamp( (int)Math.round(r_accum), 0, 255);
                short dg = (short)GeomUtils.clamp( (int)Math.round(g_accum), 0, 255);
                short db = (short)GeomUtils.clamp( (int)Math.round(b_accum), 0, 255);

                int scolor = image[row*width+column];
                short sr = RasterUtils.getR(scolor);
                short sg = RasterUtils.getG(scolor);
                short sb = RasterUtils.getB(scolor);

                float a = ((float) (mask[row*width+column] + 128) / 255);
                float a2 = 1 - a;

                if (blendMode == Mask.BlendMode.SCREEN) {
                    dr = (short)(255-(float)(255-dr)*(255-sr)/255);
                    dg = (short)(255-(float)(255-dg)*(255-sg)/255);
                    db = (short)(255-(float)(255-db)*(255-sb)/255);
                } else if (blendMode == Mask.BlendMode.MULTIPLY) {
                    dr = (short)((float)dr*sr/255);
                    dg = (short)((float)dg*sr/255);
                    db = (short)((float)db*sr/255);
                } else if (blendMode == Mask.BlendMode.OVERLAY) {
                    dr = sr < 128 ? (short)(2.0*dr*sr/255) : (short)(255-2.0*(255-dr)*(255-sr)/255);
                    dg = sg < 128 ? (short)(2.0*dg*sg/255) : (short)(255-2.0*(255-dg)*(255-sg)/255);
                    db = sb < 128 ? (short)(2.0*db*sb/255) : (short)(255-2.0*(255-db)*(255-sb)/255);
                }

                sr = (short) (sr * a2 + dr * a);
                sg = (short) (sg * a2 + dg * a);
                sb = (short) (sb * a2 + db * a);

                image[row*width+column] = RasterUtils.asRGB(sr,sg,sb,255);
            }
        }
    }

    public static void drawOver( byte[] image, int image_width, int image_height, byte[] tex, int tex_width, int tex_height, Envelope envelope, boolean tile ) {
        //TextureBlender.drawOverTiled(image,image_width,image_height,tex,tex_width,tex_height,envelope.getMinX(),envelope.getMaxX(),envelope.getMinY(),envelope.getMaxY());

        for (int column = 0; column != image_width; ++column) {
            for (int row = 0; row != image_height; ++row) {

                double gray_accum = 0;
                int count = 0;

                for (int interp_x = 0; interp_x != 4; ++interp_x) {
                    for (int interp_y = 0; interp_y != 4; ++interp_y) {
                        double x = column+0.25*interp_x+0.125;
                        double y = row+0.25*interp_y+0.125;
                        byte c = interpolate(envelope,tex,tex_width,tex_height,x,y,tile);
                        gray_accum += c;
                        count += 1;
                    }
                }

                if (count != 0) {
                    gray_accum /= count;
                }

                byte gray = (byte)GeomUtils.clamp( (int)Math.round(gray_accum), -128, 127);

                image[row*image_width+column] = gray;
            }
        }
    }

}
