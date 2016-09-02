package Utils;

import com.vividsolutions.jts.math.Vector2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Created by Artyom.Fomenko on 05.08.2016.
 */
public class RasterUtils {

    /**
     * Performs rasterization of line into double buffer
     *
     * @param buf
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @param color
     */
    public static void rasterizeline(double[][] buf, int x,int y,int x2, int y2, double color) {
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

    public static void applyAlong(Consumer<Pair<Integer,Integer>> cons, int x, int y, int x2, int y2) {
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
        Pair<Integer,Integer> consumerPair = new Pair<>(0,0);
        for (int i=0;i<=longest;i++) {
            consumerPair.v1 = x;
            consumerPair.v2 = y;
            cons.accept(consumerPair);
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

    public static void flush(double[][] target, double value) {
        int columns_number = target[0].length;
        for (int row = 0; row != target.length; ++row) {
            for (int column = 0; column != columns_number; ++column) {
                target[row][column] = value;
            }
        }
    }

    public static void flush(int[][] target, int value) {
        int columns_number = target[0].length;
        for (int row = 0; row != target.length; ++row) {
            for (int column = 0; column != columns_number; ++column) {
                target[row][column] = value;
            }
        }
    }

    public static void flush(short[][] target, short value) {
        int columns_number = target[0].length;
        for (int row = 0; row != target.length; ++row) {
            for (int column = 0; column != columns_number; ++column) {
                target[row][column] = value;
            }
        }
    }

    /**
     * Performs rasterization of line into int buffer
     *
     * @param buf
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @param color
     */
    public static void rasterizeline(int[][] buf, int x,int y,int x2, int y2, int color) {
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

    public static double[][] sobel(double[][] buf) {
        double[][] result = new double[buf.length][buf[0].length];

        for (int row = 1; row < buf.length-1; ++ row) {
            for (int column = 1; column < buf[row].length-1; ++column) {
                double val1 = 0;
                val1+=buf[row-1][column-1]*+3;
                val1+=buf[row-1][column  ]*+10;
                val1+=buf[row-1][column+1]*+3;
                //val1+=buf[row  ][column-1]*0;
                //val1+=buf[row  ][column  ]*0;
                //val1+=buf[row  ][column+1]*0;
                val1+=buf[row+1][column-1]*-3;
                val1+=buf[row+1][column  ]*-10;
                val1+=buf[row+1][column+1]*-3;
                val1 = Math.abs(val1/16);

                double val2 = 0;
                val2+=buf[row-1][column-1]*+3;
                //val2+=buf[row-1][column  ]*0;
                val2+=buf[row-1][column+1]*-3;
                val2+=buf[row  ][column-1]*+10;
                //val2+=buf[row  ][column  ]*0;
                val2+=buf[row  ][column+1]*-10;
                val2+=buf[row+1][column-1]*+3;
                //val2+=buf[row+1][column  ]*0;
                val2+=buf[row+1][column+1]*-3;
                val2 = Math.abs(val2/16);
                result [row][column] = Math.sqrt(val1*val1+val2*val2);
            }
        }

        return result;
    }

    public static float[] sobel(float[] buf, int width, int height) {
        float[] result = new float[width*height];
        for (int row = 1; row < height-1; ++ row) {
            for (int column = 1; column < width-1; ++column) {
                double val1 = 0;
                val1+=buf[(row-1)*width+column-1]*+3;
                val1+=buf[(row-1)*width+column  ]*+10;
                val1+=buf[(row-1)*width+column+1]*+3;
                //val1+=buf[(row  )*width+column-1]*0;
                //val1+=buf[(row  )*width+column  ]*0;
                //val1+=buf[(row  )*width+column+1]*0;
                val1+=buf[(row+1)*width+column-1]*-3;
                val1+=buf[(row+1)*width+column  ]*-10;
                val1+=buf[(row+1)*width+column+1]*-3;
                val1 = Math.abs(val1/16);

                double val2 = 0;
                val2+=buf[(row-1)*width+column-1]*+3;
                //val2+=buf[(row-1)*width+column  ]*0;
                val2+=buf[(row-1)*width+column+1]*-3;
                val2+=buf[(row  )*width+column-1]*+10;
                //val2+=buf[(row  )*width+column  ]*0;
                val2+=buf[(row  )*width+column+1]*-10;
                val2+=buf[(row+1)*width+column-1]*+3;
                //val2+=buf[(row+1)*width+column  ]*0;
                val2+=buf[(row+1)*width+column+1]*-3;
                val2 = Math.abs(val2/16);
                result[(row)*width+column] = (float)Math.sqrt(val1*val1+val2*val2);
            }
        }

        for (int column = 0; column < width; ++column) {
            result[column] = result[(2)*width+column];
            result[(1)*width+column] = result[(2)*width+column];
            result[(height-1)*width+column] = result[(height-3)*width+column];
            result[(height-2)*width+column] = result[(height-3)*width+column];
        }

        for (int row = 1; row < height-1; ++row) {
            result[row*width] = result[row*width+2];
            result[row*width+1] = result[row*width+2];
            result[row*width+width-1] = result[row*width+width-3];
            result[row*width+width-2] = result[row*width+width-3];
        }

        return result;
    }

    public static Pair<Double,Double> getMinMax(float[] buf) {
        double min = buf[0];
        double max = buf[0];
        for (int i = 0; i != buf.length; ++i) {
            if (buf[i] < min) {
                min = buf[i];
            } else if (buf[i] > max) {
                max = buf[i];
            }
        }
        return new Pair<>(min,max);
    }

    public static Pair<Double,Double> getMinMax(double[] buf) {
        double min = buf[0];
        double max = buf[0];
        for (int i = 0; i != buf.length; ++i) {
            if (buf[i] < min) {
                min = buf[i];
            } else if (buf[i] > max) {
                max = buf[i];
            }
        }
        return new Pair<>(min,max);
    }


    public static void map(float[] buf, double in_min, double in_max, double out_min, double out_max) {
        for (int i = 0; i != buf.length; ++i) {
            buf[i] = (float)GeomUtils.map(buf[i],in_min,in_max,out_min,out_max);
        }
    }

    public static void map(double[] buf, double in_min, double in_max, double out_min, double out_max) {
        for (int i = 0; i != buf.length; ++i) {
            buf[i] = GeomUtils.map(buf[i],in_min,in_max,out_min,out_max);
        }
    }


    public static void map(float[] buf, double out_min, double out_max) {
        Pair<Double,Double> min_max_pair = getMinMax(buf);
        map(buf,min_max_pair.v1,min_max_pair.v2,out_min,out_max);
    }

    public static void map(double[] buf, double out_min, double out_max) {
        Pair<Double,Double> min_max_pair = getMinMax(buf);
        map(buf,min_max_pair.v1,min_max_pair.v2,out_min,out_max);
    }


    public static void angleSobelBased(float[] buf, float[] normal, float[] angle, int width, int height, double width_scale, double height_scale) {
        Vector2D pivot_vector = new Vector2D(0,1);
        for (int row = 1; row < height-1; ++ row) {
            for (int column = 1; column < width-1; ++column) {
                double val1 = 0;
                val1+=buf[(row-1)*width+column-1]*+3;
                val1+=buf[(row-1)*width+column  ]*+10;
                val1+=buf[(row-1)*width+column+1]*+3;
                //val1+=buf[(row  )*width+column-1]*0;
                //val1+=buf[(row  )*width+column  ]*0;
                //val1+=buf[(row  )*width+column+1]*0;
                val1+=buf[(row+1)*width+column-1]*-3;
                val1+=buf[(row+1)*width+column  ]*-10;
                val1+=buf[(row+1)*width+column+1]*-3;

                double val2 = 0;
                val2+=buf[(row-1)*width+column-1]*+3;
                //val2+=buf[(row-1)*width+column  ]*0;
                val2+=buf[(row-1)*width+column+1]*-3;
                val2+=buf[(row  )*width+column-1]*+10;
                //val2+=buf[(row  )*width+column  ]*0;
                val2+=buf[(row  )*width+column+1]*-10;
                val2+=buf[(row+1)*width+column-1]*+3;
                //val2+=buf[(row+1)*width+column  ]*0;
                val2+=buf[(row+1)*width+column+1]*-3;

                val1 = val1/height_scale/16;
                val2 = val2/width_scale/16;

                Vector2D vec = new Vector2D(val1,val2);
                double len = vec.length();
                normal[(row)*width+column] = (float)len;
                angle[(row)*width+column] = (float)Math.abs(vec.angleTo(pivot_vector));
            }
        }

    }

    public static void padding(float[] buf, int width, int height, int paddingSize) {

        for (int column = 0; column != width; ++ column) {
            int row;
            for (int padding = 0; padding != paddingSize; ++padding) {
                row = 0;
                buf[(row+padding)*width+column] = buf[(row+paddingSize)*width+column];
                row = height-1;
                buf[(row-padding)*width+column] = buf[(row-paddingSize)*width+column];
            }
        }

        for (int row = 0; row != height; ++row) {
            int column;
            for (int padding = 0; padding != paddingSize; ++padding) {
                column = 0;
                buf[row*width+column+padding] = buf[row*width+column+paddingSize];
                column = width-1;
                buf[row*width+column-padding] = buf[row*width+column-paddingSize];
            }
        }

    }

    public static void padding(byte[] buf, int width, int height, int paddingSize) {

        for (int column = 0; column != width; ++ column) {
            int row;
            for (int padding = 0; padding != paddingSize; ++padding) {
                row = 0;
                buf[(row+padding)*width+column] = buf[(row+paddingSize)*width+column];
                row = height-1;
                buf[(row-padding)*width+column] = buf[(row-paddingSize)*width+column];
            }
        }

        for (int row = 0; row != height; ++row) {
            int column;
            for (int padding = 0; padding != paddingSize; ++padding) {
                column = 0;
                buf[row*width+column+padding] = buf[row*width+column+paddingSize];
                column = width-1;
                buf[row*width+column-padding] = buf[row*width+column-paddingSize];
            }
        }

    }

    public static void padding(double[] buf, int width, int height, int paddingSize) {

        for (int column = 0; column != width; ++ column) {
            int row;
            for (int padding = 0; padding != paddingSize; ++padding) {
                row = 0;
                buf[(row+padding)*width+column] = buf[(row+paddingSize)*width+column];
                row = height-1;
                buf[(row-padding)*width+column] = buf[(row-paddingSize)*width+column];
            }
        }

        for (int row = 0; row != height; ++row) {
            int column;
            for (int padding = 0; padding != paddingSize; ++padding) {
                column = 0;
                buf[row*width+column+padding] = buf[row*width+column+paddingSize];
                column = width-1;
                buf[row*width+column-padding] = buf[row*width+column-paddingSize];
            }
        }

    }

    public static void gauss(double[][] result, int blurSize, int iterations) {
        int rowCount = result.length;
        int columnCount = result[0].length;
        for (int i = 0; i != iterations; ++i) {
            for (int row = 0; row != rowCount; ++row) {
                double[] gaussian_buf = new double[columnCount];
                for (int column = 0; column != columnCount; ++column) {
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += result[row][i2]/size;
                    }
                }
                // Put buf to resukt
                for (int column = 0; column != columnCount; ++column) {
                    result[row][column] = gaussian_buf[column];
                }
            }

            for (int column = 0; column != columnCount; ++column) {
                double[] gaussian_buf = new double[rowCount];
                for (int row = 1; row != rowCount - 1; ++row) {
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += result[i2][column]/size;
                    }
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    result[row][column] = gaussian_buf[row];
                }
            }
        }
    }

    public static void gauss(double[][] result, int[][] sizeMatrix, int iterations) {
        int rowCount = result.length;
        int columnCount = result[0].length;
        for (int i = 0; i != iterations; ++i) {
            for (int row = 0; row != rowCount; ++row) {
                double[] gaussian_buf = new double[columnCount];
                for (int column = 0; column != columnCount; ++column) {
                    int blurSize = sizeMatrix[row][column];
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += result[row][i2]/size;
                    }
                }
                // Put buf to result
                for (int column = 0; column != columnCount; ++column) {
                    result[row][column] = gaussian_buf[column];
                }
            }

            for (int column = 0; column != columnCount; ++column) {
                double[] gaussian_buf = new double[rowCount];
                for (int row = 1; row != rowCount - 1; ++row) {
                    int blurSize = sizeMatrix[row][column];
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += result[i2][column]/size;
                    }
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    result[row][column] = gaussian_buf[row];
                }
            }
        }
    }

    public static void gauss(byte[] pixels, int width, int height, int blurSize, int iterations) {
        int rowCount = height;
        int columnCount = width;
        for (int i = 0; i != iterations; ++i) {
            int[] gaussian_buf = new int[columnCount];
            for (int row = 0; row != rowCount; ++row) {
                for (int column = 0; column != columnCount; ++column) {
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += (pixels[row*width+i2] );
                    }
                    gaussian_buf[column]=gaussian_buf[column]/size;
                }
                // Put buf to result
                for (int column = 0; column != columnCount; ++column) {
                    pixels[row*width+column] = (byte) gaussian_buf[column];
                }
            }

            gaussian_buf = new int[rowCount];
            for (int column = 0; column != columnCount; ++column) {
                for (int row = 1; row != rowCount - 1; ++row) {
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += pixels[i2*width+column];
                    }
                    gaussian_buf[row]=gaussian_buf[row]/size;
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    pixels[row*width+column] = (byte) gaussian_buf[row];
                }
            }
        }
    }

    public static void gauss(float[] pixels, int width, int height, int blurSize, int iterations) {
        int rowCount = height;
        int columnCount = width;
        for (int i = 0; i != iterations; ++i) {
            float[] gaussian_buf = new float[columnCount];
            for (int row = 0; row != rowCount; ++row) {
                for (int column = 0; column != columnCount; ++column) {
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += (pixels[row*width+i2] );
                    }
                    gaussian_buf[column]=gaussian_buf[column]/size;
                }
                // Put buf to result
                for (int column = 0; column != columnCount; ++column) {
                    pixels[row*width+column] = gaussian_buf[column];
                }
            }

            gaussian_buf = new float[rowCount];
            for (int column = 0; column != columnCount; ++column) {
                for (int row = 1; row != rowCount - 1; ++row) {
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += pixels[i2*width+column];
                    }
                    gaussian_buf[row]=gaussian_buf[row]/size;
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    pixels[row*width+column] = gaussian_buf[row];
                }
            }
        }
    }

    public static void gauss(double[] pixels, int width, int height, int blurSize, int iterations) {
        int rowCount = height;
        int columnCount = width;
        for (int i = 0; i != iterations; ++i) {
            double[] gaussian_buf = new double[columnCount];
            for (int row = 0; row != rowCount; ++row) {
                for (int column = 0; column != columnCount; ++column) {
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += (pixels[row*width+i2] );
                    }
                    gaussian_buf[column]=gaussian_buf[column]/size;
                }
                // Put buf to result
                for (int column = 0; column != columnCount; ++column) {
                    pixels[row*width+column] = gaussian_buf[column];
                }
            }

            gaussian_buf = new double[rowCount];
            for (int column = 0; column != columnCount; ++column) {
                for (int row = 1; row != rowCount - 1; ++row) {
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += pixels[i2*width+column];
                    }
                    gaussian_buf[row]=gaussian_buf[row]/size;
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    pixels[row*width+column] = gaussian_buf[row];
                }
            }
        }
    }


    public static double[] linearize(double[][] in) {
        int width = in[0].length;
        int height = in.length;
        double[] out = new double[width*height];
        for (int row = 0; row != height; ++row) {
            assert in.length == width;
            for (int column = 0; column != width; ++column) {
                out[row*width+column] = in[row][column];
            }
        }
        return out;
    }

    public static float[] linearize(float[][] in) {
        int width = in[0].length;
        int height = in.length;
        float[] out = new float[width*height];
        for (int row = 0; row != height; ++row) {
            assert in.length == width;
            for (int column = 0; column != width; ++column) {
                out[row*width+column] = in[row][column];
            }
        }
        return out;
    }

    public static void bloom(float[] pixels, int width, int height, double weight, int size) {
        float[] buf = Arrays.copyOf(pixels,pixels.length);
        RasterUtils.dilate(buf, width, height, size);
        RasterUtils.gauss(buf,width,height,size*3,2);
        for (int i = 0; i != buf.length; ++i) {
            pixels[i] = Math.max((float) (buf[i] * weight), pixels[i]);
            buf[i] = pixels[i];
        }
    }

    public static void dilate(float[] pixels, int width, int height, int size) {
        int rowCount = height;
        int columnCount = width;
        float[] gaussian_buf = new float[columnCount];
        for (int row = 0; row != rowCount; ++row) {
            for (int column = 0; column != columnCount; ++column) {
                int startcolumn = GeomUtils.clamp(column-size,0,columnCount-1);
                int endColumn = GeomUtils.clamp(column+size,0,columnCount-1)+1;
                gaussian_buf[column] = pixels[row*width+startcolumn];
                for (int i2 = startcolumn; i2 < endColumn;++i2) {
                    gaussian_buf[column] = Math.max(gaussian_buf[column], pixels[row*width+i2]);
                }
            }
            // Put buf to result
            for (int column = 0; column != columnCount; ++column) {
                pixels[row*width+column] = gaussian_buf[column];
            }
        }

        gaussian_buf = new float[rowCount];
        for (int column = 0; column != columnCount; ++column) {
            for (int row = 1; row != rowCount - 1; ++row) {
                int startRow = GeomUtils.clamp(row-size,0,rowCount-1);
                int endRow = GeomUtils.clamp(row+size,0,rowCount-1)+1;
                gaussian_buf[row] = pixels[startRow*width+column];
                for (int i2 = startRow; i2 < endRow;++i2) {
                    gaussian_buf[row] = Math.max(pixels[i2*width+column],gaussian_buf[row]);
                }
            }
            // Put buf to result
            for (int row = 0; row != rowCount; ++row) {
                pixels[row*width+column] = gaussian_buf[row];
            }
        }
    }


    public static void dilate(byte[] pixels, int width, int height, int size) {
        int rowCount = height;
        int columnCount = width;
        byte[] gaussian_buf = new byte[columnCount];
        for (int row = 0; row != rowCount; ++row) {
            for (int column = 0; column != columnCount; ++column) {
                int startcolumn = GeomUtils.clamp(column-size,0,columnCount-1);
                int endColumn = GeomUtils.clamp(column+size,0,columnCount-1)+1;
                gaussian_buf[column] = pixels[row*width+startcolumn];
                for (int i2 = startcolumn; i2 < endColumn;++i2) {
                    gaussian_buf[column] = (byte)Math.max(gaussian_buf[column], pixels[row*width+i2]);
                }
            }
            // Put buf to result
            for (int column = 0; column != columnCount; ++column) {
                pixels[row*width+column] = gaussian_buf[column];
            }
        }

        gaussian_buf = new byte[rowCount];
        for (int column = 0; column != columnCount; ++column) {
            for (int row = 1; row != rowCount - 1; ++row) {
                int startRow = GeomUtils.clamp(row-size,0,rowCount-1);
                int endRow = GeomUtils.clamp(row+size,0,rowCount-1)+1;
                gaussian_buf[row] = pixels[startRow*width+column];
                for (int i2 = startRow; i2 < endRow;++i2) {
                    gaussian_buf[row] = (byte)Math.max(pixels[i2*width+column],gaussian_buf[row]);
                }
            }
            // Put buf to result
            for (int row = 0; row != rowCount; ++row) {
                pixels[row*width+column] = gaussian_buf[row];
            }
        }
    }


    public static void dilate(double[] pixels, int width, int height, int size) {
        int rowCount = height;
        int columnCount = width;
        double[] gaussian_buf = new double[columnCount];
        for (int row = 0; row != rowCount; ++row) {
            for (int column = 0; column != columnCount; ++column) {
                int startcolumn = GeomUtils.clamp(column-size,0,columnCount-1);
                int endColumn = GeomUtils.clamp(column+size,0,columnCount-1)+1;
                gaussian_buf[column] = pixels[row*width+startcolumn];
                for (int i2 = startcolumn; i2 < endColumn;++i2) {
                    gaussian_buf[column] = Math.max(gaussian_buf[column], pixels[row*width+i2]);
                }
            }
            // Put buf to result
            for (int column = 0; column != columnCount; ++column) {
                pixels[row*width+column] = gaussian_buf[column];
            }
        }

        gaussian_buf = new double[rowCount];
        for (int column = 0; column != columnCount; ++column) {
            for (int row = 1; row != rowCount - 1; ++row) {
                int startRow = GeomUtils.clamp(row-size,0,rowCount-1);
                int endRow = GeomUtils.clamp(row+size,0,rowCount-1)+1;
                gaussian_buf[row] = pixels[startRow*width+column];
                for (int i2 = startRow; i2 < endRow;++i2) {
                    gaussian_buf[row] = Math.max(pixels[i2*width+column],gaussian_buf[row]);
                }
            }
            // Put buf to result
            for (int row = 0; row != rowCount; ++row) {
                pixels[row*width+column] = gaussian_buf[row];
            }
        }
    }

    public static void erode(double[] pixels, int width, int height, int size) {
        int rowCount = height;
        int columnCount = width;
        double[] gaussian_buf = new double[columnCount];
        for (int row = 0; row != rowCount; ++row) {
            for (int column = 0; column != columnCount; ++column) {
                int startcolumn = GeomUtils.clamp(column-size,0,columnCount-1);
                int endColumn = GeomUtils.clamp(column+size,0,columnCount-1)+1;
                gaussian_buf[column] = pixels[row*width+startcolumn];
                for (int i2 = startcolumn; i2 < endColumn;++i2) {
                    gaussian_buf[column] = Math.min(gaussian_buf[column], pixels[row*width+i2]);
                }
            }
            // Put buf to result
            for (int column = 0; column != columnCount; ++column) {
                pixels[row*width+column] = gaussian_buf[column];
            }
        }

        gaussian_buf = new double[rowCount];
        for (int column = 0; column != columnCount; ++column) {
            for (int row = 1; row != rowCount - 1; ++row) {
                int startRow = GeomUtils.clamp(row-size,0,rowCount-1);
                int endRow = GeomUtils.clamp(row+size,0,rowCount-1)+1;
                gaussian_buf[row] = pixels[startRow*width+column];
                for (int i2 = startRow; i2 < endRow;++i2) {
                    gaussian_buf[row] = Math.min(pixels[i2*width+column],gaussian_buf[row]);
                }
            }
            // Put buf to result
            for (int row = 0; row != rowCount; ++row) {
                pixels[row*width+column] = gaussian_buf[row];
            }
        }
    }

    public static void erode(byte[] pixels, int width, int height, int size) {
        int rowCount = height;
        int columnCount = width;
        byte[] gaussian_buf = new byte[columnCount];
        for (int row = 0; row != rowCount; ++row) {
            for (int column = 0; column != columnCount; ++column) {
                int startcolumn = GeomUtils.clamp(column-size,0,columnCount-1);
                int endColumn = GeomUtils.clamp(column+size,0,columnCount-1)+1;
                gaussian_buf[column] = pixels[row*width+startcolumn];
                for (int i2 = startcolumn; i2 < endColumn;++i2) {
                    gaussian_buf[column] = (byte)Math.min(gaussian_buf[column], pixels[row*width+i2]);
                }
            }
            // Put buf to result
            for (int column = 0; column != columnCount; ++column) {
                pixels[row*width+column] = gaussian_buf[column];
            }
        }

        gaussian_buf = new byte[rowCount];
        for (int column = 0; column != columnCount; ++column) {
            for (int row = 1; row != rowCount - 1; ++row) {
                int startRow = GeomUtils.clamp(row-size,0,rowCount-1);
                int endRow = GeomUtils.clamp(row+size,0,rowCount-1)+1;
                gaussian_buf[row] = pixels[startRow*width+column];
                for (int i2 = startRow; i2 < endRow;++i2) {
                    gaussian_buf[row] = (byte)Math.min(pixels[i2*width+column],gaussian_buf[row]);
                }
            }
            // Put buf to result
            for (int row = 0; row != rowCount; ++row) {
                pixels[row*width+column] = gaussian_buf[row];
            }
        }
    }

    public static void gauss(float[][] result, int blurSize, int iterations) {
        int rowCount = result.length;
        int columnCount = result[0].length;
        for (int i = 0; i != iterations; ++i) {
            for (int row = 0; row != rowCount; ++row) {
                float[] gaussian_buf = new float[columnCount];
                for (int column = 0; column != columnCount; ++column) {
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += result[row][i2]/size;
                    }
                }
                // Put buf to resukt
                for (int column = 0; column != columnCount; ++column) {
                    result[row][column] = gaussian_buf[column];
                }
            }

            for (int column = 0; column != columnCount; ++column) {
                float[] gaussian_buf = new float[rowCount];
                for (int row = 1; row != rowCount - 1; ++row) {
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += result[i2][column]/size;
                    }
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    result[row][column] = gaussian_buf[row];
                }
            }
        }
    }

    public static void gauss(float[][] result, int[][] sizeMatrix, int iterations) {
        int rowCount = result.length;
        int columnCount = result[0].length;
        for (int i = 0; i != iterations; ++i) {
            for (int row = 0; row != rowCount; ++row) {
                float[] gaussian_buf = new float[columnCount];
                for (int column = 0; column != columnCount; ++column) {
                    int blurSize = sizeMatrix[row][column];
                    int startcolumn = GeomUtils.clamp(column-blurSize,0,columnCount-1);
                    int endColumn = GeomUtils.clamp(column+blurSize,0,columnCount-1)+1;
                    int size = endColumn-startcolumn;
                    gaussian_buf[column] = 0;
                    for (int i2 = startcolumn; i2 < endColumn;++i2) {
                        gaussian_buf[column] += result[row][i2]/size;
                    }
                }
                // Put buf to result
                for (int column = 0; column != columnCount; ++column) {
                    result[row][column] = gaussian_buf[column];
                }
            }

            for (int column = 0; column != columnCount; ++column) {
                float[] gaussian_buf = new float[rowCount];
                for (int row = 1; row != rowCount - 1; ++row) {
                    int blurSize = sizeMatrix[row][column];
                    int startRow = GeomUtils.clamp(row-blurSize,0,rowCount-1);
                    int endRow = GeomUtils.clamp(row+blurSize,0,rowCount-1)+1;
                    int size = endRow-startRow;
                    gaussian_buf[row] = 0;
                    for (int i2 = startRow; i2 < endRow;++i2) {
                        gaussian_buf[row] += result[i2][column]/size;
                    }
                }
                // Put buf to result
                for (int row = 0; row != rowCount; ++row) {
                    result[row][column] = gaussian_buf[row];
                }
            }
        }
    }

    public static void dilate(byte[] pixels, int width, int height) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                if (column > 0) pixels[row*width+(column-1)] = pixels[row*width+(column-1)] > pixels[row*width+column] ? pixels[row*width+(column-1)] : pixels[row*width+column];
                if (row > 0)    pixels[(row-1)*width+column] = pixels[(row-1)*width+column] > pixels[row*width+column] ? pixels[(row-1)*width+column] : pixels[row*width+column];
            }
        }

        for (int row = height-1; row != -1; --row) {
            for (int column = width-1; column != -1; --column) {
                if (column < width-1) pixels[row*width+(column+1)] = pixels[row*width+(column+1)] > pixels[row*width+column] ? pixels[row*width+(column+1)] : pixels[row*width+column];
                if (row < height-1)    pixels[(row+1)*width+column] = pixels[(row+1)*width+column] > pixels[row*width+column] ? pixels[(row+1)*width+column] : pixels[row*width+column];
            }
        }

    }

    public static void erode(byte[] pixels, int width, int height) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                if (column > 0) pixels[row*width+(column-1)] = pixels[row*width+(column-1)] < pixels[row*width+column] ? pixels[row*width+(column-1)] : pixels[row*width+column];
                if (row > 0)    pixels[(row-1)*width+column] = pixels[(row-1)*width+column] < pixels[row*width+column] ? pixels[(row-1)*width+column] : pixels[row*width+column];
            }
        }

        for (int row = height-1; row != -1; --row) {
            for (int column = width-1; column != -1; --column) {
                if (column < width-1) pixels[row*width+(column+1)] = pixels[row*width+(column+1)] < pixels[row*width+column] ? pixels[row*width+(column+1)] : pixels[row*width+column];
                if (row < height-1)    pixels[(row+1)*width+column] = pixels[(row+1)*width+column] < pixels[row*width+column] ? pixels[(row+1)*width+column] : pixels[row*width+column];
            }
        }

    }

    public static void dilate(float[] pixels, int width, int height) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                if (column > 0) pixels[row*width+(column-1)] = pixels[row*width+(column-1)] > pixels[row*width+column] ? pixels[row*width+(column-1)] : pixels[row*width+column];
                if (row > 0)    pixels[(row-1)*width+column] = pixels[(row-1)*width+column] > pixels[row*width+column] ? pixels[(row-1)*width+column] : pixels[row*width+column];
            }
        }

        for (int row = height-1; row != -1; --row) {
            for (int column = width-1; column != -1; --column) {
                if (column < width-1) pixels[row*width+(column+1)] = pixels[row*width+(column+1)] > pixels[row*width+column] ? pixels[row*width+(column+1)] : pixels[row*width+column];
                if (row < height-1)    pixels[(row+1)*width+column] = pixels[(row+1)*width+column] > pixels[row*width+column] ? pixels[(row+1)*width+column] : pixels[row*width+column];
            }
        }

    }

    public static void erode(float[] pixels, int width, int height) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                if (column > 0) pixels[row*width+(column-1)] = pixels[row*width+(column-1)] < pixels[row*width+column] ? pixels[row*width+(column-1)] : pixels[row*width+column];
                if (row > 0)    pixels[(row-1)*width+column] = pixels[(row-1)*width+column] < pixels[row*width+column] ? pixels[(row-1)*width+column] : pixels[row*width+column];
            }
        }

        for (int row = height-1; row != -1; --row) {
            for (int column = width-1; column != -1; --column) {
                if (column < width-1) pixels[row*width+(column+1)] = pixels[row*width+(column+1)] < pixels[row*width+column] ? pixels[row*width+(column+1)] : pixels[row*width+column];
                if (row < height-1)    pixels[(row+1)*width+column] = pixels[(row+1)*width+column] < pixels[row*width+column] ? pixels[(row+1)*width+column] : pixels[row*width+column];
            }
        }

    }

    public static void dilate(double[] pixels, int width, int height) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                if (column > 0) pixels[row*width+(column-1)] = pixels[row*width+(column-1)] > pixels[row*width+column] ? pixels[row*width+(column-1)] : pixels[row*width+column];
                if (row > 0)    pixels[(row-1)*width+column] = pixels[(row-1)*width+column] > pixels[row*width+column] ? pixels[(row-1)*width+column] : pixels[row*width+column];
            }
        }

        for (int row = height-1; row != -1; --row) {
            for (int column = width-1; column != -1; --column) {
                if (column < width-1) pixels[row*width+(column+1)] = pixels[row*width+(column+1)] > pixels[row*width+column] ? pixels[row*width+(column+1)] : pixels[row*width+column];
                if (row < height-1)    pixels[(row+1)*width+column] = pixels[(row+1)*width+column] > pixels[row*width+column] ? pixels[(row+1)*width+column] : pixels[row*width+column];
            }
        }

    }

    public static void erode(double[] pixels, int width, int height) {

        for (int row = 0; row != height; ++row) {
            for (int column = 0; column != width; ++column) {
                if (column > 0) pixels[row*width+(column-1)] = pixels[row*width+(column-1)] < pixels[row*width+column] ? pixels[row*width+(column-1)] : pixels[row*width+column];
                if (row > 0)    pixels[(row-1)*width+column] = pixels[(row-1)*width+column] < pixels[row*width+column] ? pixels[(row-1)*width+column] : pixels[row*width+column];
            }
        }

        for (int row = height-1; row != -1; --row) {
            for (int column = width-1; column != -1; --column) {
                if (column < width-1) pixels[row*width+(column+1)] = pixels[row*width+(column+1)] < pixels[row*width+column] ? pixels[row*width+(column+1)] : pixels[row*width+column];
                if (row < height-1)    pixels[(row+1)*width+column] = pixels[(row+1)*width+column] < pixels[row*width+column] ? pixels[(row+1)*width+column] : pixels[row*width+column];
            }
        }

    }

    public static void saveAsTxt(double[][] buffer, String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }
        int y_steps = buffer.length;
        int x_steps = buffer[0].length;

        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                out.print(buffer[i][j]+" ");
            }
            out.println();
        }
        out.close();

    }


    public static void saveAsTxt(short[][] buffer, String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }
        int y_steps = buffer.length;
        int x_steps = buffer[0].length;

        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                out.print(buffer[i][j]+" ");
            }
            out.println();
        }
        out.close();

    }

    public static void saveAsTxt(float[] buffer, int width, int height, String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }

        for (int row = height-1; row >= 0; --row) {
            for (int column = 0; column != width; ++column) {
                out.print(buffer[row*width+column]+" ");
            }
            out.println();
        }
        out.close();

    }

    public static void saveAsTxt(byte[] buffer, int width, int height, String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }

        for (int row = height-1; row >= 0; --row) {
            for (int column = 0; column != width; ++column) {
                out.print(buffer[row*width+column]+" ");
            }
            out.println();
        }
        out.close();

    }

    public static short getR(int color) {
        return (short)((color>>16)&0xFF);
    }

    public static short getG(int color) {
        return (short)((color>>8)&0xFF);
    }

    public static short getB(int color) {
        return (short)((color>>0)&0xFF);
    }

    public static int asRGB(int r, int g, int b) {
        return (r << 16) + (g << 8) + b;
    }

    public static int asRGB(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | (b);
    }

    public static Pair<Integer,float[]> loadTextAsGrayscaleFloat(String path) {

        int width = 0;
        int height = 0;

        ArrayList<float[]> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path)))
        {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {

                String[] tokens = sCurrentLine.split("\\s+");

                if (tokens.length == 0) continue;

                if (width == 0) {
                    width = tokens.length;
                } else {
                    if (tokens.length != width) throw new RuntimeException("Invalid text file");
                }

                float[] line = new float[width];

                for (int i = 0; i != tokens.length; ++i) {
                    line[i] = (float)(Double.parseDouble(tokens[i]));
                }

                lines.add(line);
            }
            height = lines.size();

            float[] ret = new float[width*height];
            int row = 0;
            for (float[] line : lines) {
                for (int column = 0; column != width; ++column) {
                    ret[(height-row-1)*width+column] = line[column];
                }
                row += 1;
            }

            return new Pair<>(width,ret);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Pair<Integer,float[]> loadARGBasGrayscaleFloat(String path) {

        BufferedImage img;
        float[] ret;
        try {
            String filename = path;
            img = ImageIO.read(new File(filename));
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
            return null;
        };
        int width = img.getWidth();
        int height = img.getHeight();
        ret = new float[width*height];
        for (int i = 0; i != ret.length; ++i) {
            int rgb = img.getRGB(i%width,height-1-i/width);
            ret[i] = (float)(getR(rgb)+getG(rgb)+getB(rgb))/3/255;
        }
        return new Pair<>(width, ret);
    }

    public static Pair<Integer,double[]> loadARGBasGrayscaleDouble(String path) {

        BufferedImage img;
        double[] ret;
        try {
            String filename = path;
            img = ImageIO.read(new File(filename));
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
            return null;
        };
        int width = img.getWidth();
        int height = img.getHeight();
        ret = new double[width*height];
        for (int i = 0; i != ret.length; ++i) {
            int rgb = img.getRGB(i%width,i/width);
            ret[i] = (double)(getR(rgb)+getG(rgb)+getB(rgb))/3/255;
        }
        return new Pair<>(width, ret);
    }


    public static void save(BufferedImage image, String path) {

        String extenstion = OutputUtils.getExtension(path);
        try {
            File f = new File(path);
            ImageIO.write(image, extenstion, f);
        } catch (IOException e) {
            throw new RuntimeException("Could not save " + path);
        }

    }

    public static void save(double[] buf, int width, int height, String path) {
        String extension = OutputUtils.getExtension(path);

        double minHeight = buf[0], maxHeight = buf[0];
        for (int y = height-1; y >= 0; --y) {
            for (int x = 0; x != width; ++x) {
                minHeight = Math.min(minHeight, buf[y*width + x]);
                maxHeight = Math.max(maxHeight, buf[y*width + x]);
            }
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = height-1; y >= 0; --y) {
            for (int x = 0; x != width; ++x) {
                int grey = 255-(int)GeomUtils.map(buf[y*width+x], minHeight, maxHeight, 255, 0);
                image.setRGB(x, height-y-1, (((grey << 8) + (int)(grey)) << 8) + (int)(grey));
            }
        }

        save(image,path);
    }

    public static void save(float[] buf, int width, int height, String path) {
        String extension = OutputUtils.getExtension(path);

        double minHeight = buf[0], maxHeight = buf[0];
        for (int y = height-1; y >= 0; --y) {
            for (int x = 0; x != width; ++x) {
                minHeight = Math.min(minHeight, buf[y*width + x]);
                maxHeight = Math.max(maxHeight, buf[y*width + x]);
            }
        }
        save(buf,width,height,minHeight,maxHeight,path);
    }

    public static void save(float[] buf, int width, int height, double minHeight, double maxHeight, String path) {
        String extension = OutputUtils.getExtension(path);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = height-1; y >= 0; --y) {
            for (int x = 0; x != width; ++x) {
                int grey = 255-(int)GeomUtils.map(buf[y*width+x], minHeight, maxHeight, 255, 0);
                image.setRGB(x, height-y-1, (((grey << 8) + (int)(grey)) << 8) + (int)(grey));
            }
        }

        save(image,path);
    }

    public static void save(byte[] buf, int width, int height, String path) {
        String extension = OutputUtils.getExtension(path);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = height-1; y >= 0; --y) {
            for (int x = 0; x != width; ++x) {
                int grey = buf[y*width+x]+128;
                image.setRGB(x, height-y-1, (((grey << 8) + (int)(grey)) << 8) + (int)(grey));
            }
        }

        save(image,path);
    }


    public static void saveAsPng(float[][] buffer, String path) {

        int y_steps = buffer.length;
        int x_steps = buffer[0].length;

        //getting bounds of possible height values
        double minHeight = buffer[0][0], maxHeight = buffer[0][0];
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                minHeight = Math.min(minHeight, buffer[i][j]);
                maxHeight = Math.max(maxHeight, buffer[i][j]);
            }
        }

        //creating visual heightmap
        BufferedImage image = new BufferedImage(x_steps, y_steps, BufferedImage.TYPE_INT_RGB);
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                int grey = 255-(int)GeomUtils.map(buffer[i][j], minHeight, maxHeight, 255, 0);
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
    }

    public static void saveAsPng(double[][] buffer, String path) {

        int y_steps = buffer.length;
        int x_steps = buffer[0].length;

        //getting bounds of possible height values
        double minHeight = buffer[0][0], maxHeight = buffer[0][0];
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                minHeight = Math.min(minHeight, buffer[i][j]);
                maxHeight = Math.max(maxHeight, buffer[i][j]);
            }
        }

        //creating visual heightmap
        BufferedImage image = new BufferedImage(x_steps, y_steps, BufferedImage.TYPE_INT_RGB);
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                int grey = 255-(int)GeomUtils.map(buffer[i][j], minHeight, maxHeight, 255, 0);
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
    }

    public static void saveAsPng(byte[][] r, byte[][] g, byte[][] b, String path) {

        int y_steps = r.length;
        int x_steps = r[0].length;

        //creating visual heightmap
        BufferedImage image = new BufferedImage(x_steps, y_steps, BufferedImage.TYPE_INT_RGB);
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                //int grey = 255-(int)GeomUtils.map(buffer[i][j], minHeight, maxHeight, 255, 0);
                image.setRGB(j, y_steps-i-1, (((r[i][j] << 8) + (int)(g[i][j])) << 8) + (int)(b[i][j]));
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
