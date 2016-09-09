package Utils;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.function.Consumer;

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

    public PointRasterizer(double x_addition, double y_addition, double x_mult, double y_mult, int x_last_index, int y_last_index) {
        this.x_addition = x_addition;
        this.y_addition = y_addition;
        this.x_mult = x_mult;
        this.y_mult = y_mult;
        this.x_last_index = x_last_index;
        this.y_last_index = y_last_index;
    }

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

    public PointRasterizer subDivision(int startRow,int startColumn, int endRow, int endColumn) {
        return new PointRasterizer(x_addition-toX(startColumn),y_addition-toY(startRow),x_mult,y_mult,endColumn,endRow);
    }

    public int getRowCount() {
        return y_last_index+1;
    }

    public int getColumnCount() {
        return x_last_index+1;
    }

    public boolean isInBounds(double x, double y) {
        int column = (int)((x+x_addition)*x_mult);
        int row = (int)((y+y_addition)*y_mult);
        if (column < 0 || column > x_last_index || row < 0 || row > y_last_index) return false;
        return true;
    }

    public int toColumn(double x){
        int ret = (int)((x+x_addition)*x_mult);
        return ret;
    }

    public int toRow(double y) {
        int ret = (int)((y+y_addition)*y_mult);
        return ret;
    }

    public double toColumnDouble(double x){
        return ((x+x_addition)*x_mult);
    }

    public double toRowDouble(double y) {
        return ((y+y_addition)*y_mult);
    }


    public int toColumnClamped(double x){
        int ret = (int)((x+x_addition)*x_mult);
        if (ret < 0) return 0;
        if (ret > x_last_index) return x_last_index;
        return ret;
    }

    public int toRowClamped(double y) {
        int ret = (int)((y+y_addition)*y_mult);
        if (ret < 0) return 0;
        if (ret > y_last_index) return y_last_index;
        return ret;
    }

    public void rasterize(double[][] buf, LineSegment ls, double color) {
        RasterUtils.rasterizeline(buf, toRowClamped(ls.p0.y), toColumnClamped(ls.p0.x), toRowClamped(ls.p1.y), toColumnClamped(ls.p1.x), color );
    }


    public void rasterize(double[][] buf, LineString string, double color) {
        LineSegment linebuf = new LineSegment();
        LineStringIterator it = new LineStringIterator(string,linebuf);
        while (it.hasNext()) {
            it.next();
            rasterize(buf,linebuf,color);
        }
    }

    public void applyAlong(Consumer<Pair<Integer,Integer>> cons, LineSegment ls) {
        RasterUtils.applyAlong(cons, toRow(ls.p0.y), toColumn(ls.p0.x), toRow(ls.p1.y), toColumn(ls.p1.x) );
    }

    public void applyAlong(Consumer<Pair<Integer,Integer>> cons, LineString string) {
        LineSegment linebuf = new LineSegment();
        LineStringIterator it = new LineStringIterator(string, linebuf);
        while (it.hasNext()) {
            it.next();
            applyAlong(cons, linebuf);
        }
    }

    public void fillPrepass(short[][] buf, Polygon p) {
        LineString exteriorRing = (LineString)p.getExteriorRing().reverse();
        fillPrepassRing(buf,exteriorRing,false);
        for (int i = 0; i != p.getNumInteriorRing(); ++i) {
            LineString interiorRing = (LineString)p.getInteriorRingN(i).reverse();
            fillPrepassRing(buf,interiorRing,true);
        }
    }

    public void fillPrepassRing(short[][] buf, LineString ring, boolean hole) {

        final int mult = ( (CGAlgorithms.signedArea(ring.getCoordinateSequence()) < 0) ^ hole ? -1 : 1);

        ArrayList<Pair<Integer,Integer>> coordinates = new ArrayList<>();
        applyAlong((x)->{
                    coordinates.add(new Pair<>(x.v1,x.v2));
                },
                ring);

        int prev_row = -1;

        int width = buf[0].length;
        int height = buf.length;

        for (int i = 0; i < coordinates.size(); ++i) {
            int row = GeomUtils.clamp( coordinates.get(i).v1, 0, height-1);
            int column = GeomUtils.clamp( coordinates.get(i).v2, 0, width-1);
            if (row > prev_row)
                buf[row][column] += mult;
            prev_row = row;
        }

        prev_row = -1;
        for (int i = coordinates.size()-1; i >= 0; --i) {
            int row = GeomUtils.clamp(coordinates.get(i).v1, 0, height-1);
            int column = GeomUtils.clamp(coordinates.get(i).v2, 0, width-1);
            if (row > prev_row)
                buf[row][column] -= mult;
            prev_row = row;
        }
    }

//    private void fillFinalPass(float[][] buf, short[][] mask, float insideColor) {
//        for (int row = 0; row != getRowCount(); ++row) {
//            int counter = 0;
//            for (int column = 0; column != getColumnCount(); ++column) {
//                if (mask[row][column] > 0)
//                    counter+=mask[row][column];
//                if (counter != 0) buf[row][column] = insideColor;
//                if (mask[row][column] < 0)
//                    counter+=mask[row][column];
//            }
//        }
//    }

//    public void fillPolygons(float[][] buf, List<Polygon> polygons, float color) {
//
//        short[][] mask = createShortBuffer();
//        for (Polygon p : polygons) {
//            fillPrepass(mask,p);
//        };
//        fillFinalPass(buf,mask,color);
//    }
//
//    public void fillPolygons(float[][] buf, Geometry polygons, float color) {
//
//        short[][] mask = createShortBuffer();
//        for (int i = 0; i != polygons.getNumGeometries(); ++i) {
//            Polygon p = (Polygon)polygons.getGeometryN(i);
//            fillPrepass(mask,p);
//        }
//        fillFinalPass(buf,mask,color);
//    }
//
//    public void putPoint(float[][] buf, Coordinate p, float color) {
//        int column = toColumnClamped(p.x);
//        int row = toRowClamped(p.y);
//        if (column < 0 || column >= getColumnCount()) return;
//        if (row < 0 || row >= getRowCount()) return;
//        buf[row][column] = color;
//    }

    public double toX(int column) {
        return ((double)(column+0.5)/x_mult)-x_addition;
    }

    public double toY(int row) {
        return ((double)(row+0.5)/y_mult)-y_addition;
    }

    public boolean isInBounds(int row, int column) {
        return row >= 0 && row < getRowCount() && column >= 0 && column < getColumnCount();
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

    public float[][] createFloatBuffer() {
        return new float[getRowCount()][getColumnCount()];
    }

    public int[][] createIntBuffer(int initValue) {
        int[][] buf = new int[getRowCount()][getColumnCount()];
        RasterUtils.flush(buf,initValue);
        return buf;
    }


    public short[][] createShortBuffer() {
        return new  short[getRowCount()][getColumnCount()];
    }

    public short[][] createShortBuffer(short initValue) {
        short[][] buf = new short[getRowCount()][getColumnCount()];
        RasterUtils.flush(buf,initValue);
        return buf;
    }

}