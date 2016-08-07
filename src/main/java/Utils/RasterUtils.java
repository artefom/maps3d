package Utils;

/**
 * Created by Artyom.Fomenko on 05.08.2016.
 */
public class RasterUtils {
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
}
