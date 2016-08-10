package Utils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineSegment;

/**
 * Class for easy converting double coordinates to cell's row and column indexes
 */
public class PointRasterizer {

    private double x_addition;
    private double y_addition;
    private double x_mult;
    private double y_mult;
    private int x_last_index;
    private int y_last_index;

    public PointRasterizer(int x_cells, int y_cells, Envelope envelope) {
        x_addition = -envelope.getMinX();
        y_addition = -envelope.getMinY();
        x_mult = x_cells/envelope.getWidth();
        y_mult = y_cells/envelope.getHeight();
        this.x_last_index = x_cells-1;
        this.y_last_index = y_cells-1;
    }

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

    public void rasterize(double[][] buf, LineSegment ls, double color) {
        RasterUtils.rasterizeline(buf, toRow(ls.p0.y), toColumn(ls.p0.x), toRow(ls.p1.y), toColumn(ls.p1.x), color );
    }

    public double toX(int column) {
        return ((double)(column+0.5)/x_mult)-x_addition;
    }

    public double toY(int row) {
        return ((double)(row+0.5)/y_mult)-y_addition;
    }


    public double[][] createDoubleBuffer() {
        return new double[getRowCount()][getColumnCount()];
    }

    public int[][] createIntBuffer() {
        return new int[getRowCount()][getColumnCount()];
    }

    public double[][] createDoubleBuffer(double initValue) {
        double[][] buf = new double[getRowCount()][getColumnCount()];
        RasterUtils.flush(buf,initValue);
        return buf;
    }

    public int[][] createIntBuffer(int initValue) {
        int[][] buf = new int[getRowCount()][getColumnCount()];
        RasterUtils.flush(buf,initValue);
        return buf;
    }

}