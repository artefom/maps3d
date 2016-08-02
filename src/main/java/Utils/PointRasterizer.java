package Utils;

import com.vividsolutions.jts.geom.Envelope;

public class PointRasterizer {

    private double x_addition;
    private double y_addition;
    private double x_mult;
    private double y_mult;
    private int x_last_index;
    private int y_last_index;

    public PointRasterizer(double cellSize, Envelope envelope) {
        int x_cells = (int)Math.ceil(envelope.getWidth()/cellSize);
        int y_cells = (int)Math.ceil(envelope.getHeight()/cellSize);
        x_addition = -envelope.getMinX();
        y_addition = -envelope.getMinY();
        x_mult = x_cells/envelope.getWidth();
        y_mult = y_cells/envelope.getHeight();
        this.x_last_index = x_cells-1;
        this.y_last_index = y_cells-1;
    }

    public int getRowCount() {
        return y_last_index+1;
    }

    public int getColumnCount() {
        return x_last_index+1;
    }

    public int toColumn(double x){
        int ret = (int)((x+x_addition)*x_mult);
        if (ret < 0) return 0;
        if (ret > x_last_index) return x_last_index;
        return ret;
    }

    public int toRow(double y) {
        int ret = (int)((y+y_addition)*y_mult);
        if (ret < 0) return 0;
        if (ret > y_last_index) return y_last_index;
        return ret;
    }

    public double toX(int column) {
        return ((double)(column+0.5)/x_mult)-x_addition;
    }

    public double toY(int row) {
        return ((double)(row+0.5)/y_mult)-y_addition;
    }

    public void flush(double[][] target, double value) {
        int columns_number = target[0].length;
        for (int row = 0; row != target.length; ++row) {
            for (int column = 0; column != columns_number; ++column) {
                target[row][column] = value;
            }
        }
    }

    public void flush(int[][] target, int value) {
        int columns_number = target[0].length;
        for (int row = 0; row != target.length; ++row) {
            for (int column = 0; column != columns_number; ++column) {
                target[row][column] = value;
            }
        }
    }


    public double[][] createDoubleBuffer() {
        return new double[getRowCount()][getColumnCount()];
    }

    public int[][] createIntBuffer() {
        return new int[getRowCount()][getColumnCount()];
    }

    public double[][] createDoubleBuffer(double initValue) {
        double[][] buf = new double[getRowCount()][getColumnCount()];
        flush(buf,initValue);
        return buf;
    }

    public int[][] createIntBuffer(int initValue) {
        int[][] buf = new int[getRowCount()][getColumnCount()];
        flush(buf,initValue);
        return buf;
    }

}