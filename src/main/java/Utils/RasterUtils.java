package Utils;

import Isolines.IIsoline;
import com.vividsolutions.jts.algorithm.InteriorPointArea;
import com.vividsolutions.jts.geom.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public static void saveAsTxt(double[][] buffer, String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(path + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".txt");
        }
        int y_steps = buffer.length;
        int x_steps = buffer[0].length;

        System.out.println("Writing to file");
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                out.print(buffer[i][j]+" ");
            }
            out.println();
        }
        out.close();

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
}
