package Utils.Area;

import Utils.GeomUtils;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Artyom.Fomenko on 22.08.2016.
 */
public abstract class AreaBuffer<T> {

    public int width;
    public int height;


    private double envelope_Dilated_minX;
    private double envelope_Dilated_maxX;
    private double envelope_Dilated_minY;
    private double envelope_Dilated_maxY;

    private double envelope_minX;
    private double envelope_maxX;
    private double envelope_minY;
    private double envelope_maxY;

    public ArrayList<T>[] cells;

    boolean initialized;

    public void init(int width, int height) {
        if (initialized) return;
        this.width = width;
        this.height = height;
        cells = new ArrayList[width * height];
        for (int i = 0; i != cells.length; ++i) {
            cells[i] = new ArrayList<>(16);
        }
        initialized = true;
    }


    public AreaBuffer() {
        initialized = false;
    }

    public final ArrayList<T> getCell(int x, int y) {
        if (!initialized) throw new RuntimeException("Buffer not initialized!");
        return cells[y*width+x];
    }

    public final void putToCell(int x, int y, T obj) {
        if (!initialized) throw new RuntimeException("Buffer not initialized!");
        cells[y*width+x].add(obj);
    }

    public final double toLocalX(double x) {
        if (!initialized || !envelopeInitialized) throw new RuntimeException("Cell, or envelope not initialized!");
        return GeomUtils.map(x, envelope_Dilated_minX, envelope_Dilated_maxX,0,width);
    }

    public final double toLocalY(double y) {
        if (!initialized || !envelopeInitialized) throw new RuntimeException("Cell, or envelope not initialized!");
        return GeomUtils.map(y, envelope_Dilated_minY, envelope_Dilated_maxY,0,height);
    }

    public final double toGlobalX(double x) {
        if (!initialized || !envelopeInitialized) throw new RuntimeException("Cell, or envelope not initialized!");
        return GeomUtils.map(x,0,width, envelope_Dilated_minX, envelope_Dilated_maxX);
    }

    public final double toGlobalY(double y) {
        if (!initialized || !envelopeInitialized) throw new RuntimeException("Cell, or envelope not initialized!");
        return GeomUtils.map(y,0,height, envelope_Dilated_minY, envelope_Dilated_maxY);
    }


    public abstract boolean add(T entity);

    public abstract boolean remove(T entity);

    public final void addAll(Collection<T> entities) {

        for (T ent : entities) {
            add(ent);
        }

    }

    public abstract void setEnvelope(Collection<T> entities);

    /**
     * WARNING: COULD ONLY BE USED BEFORE INITIALIZATION
     * @param x_min
     * @param x_max
     * @param y_min
     * @param y_max
     */
    public void setEnvelope( double x_min, double x_max, double y_min, double y_max ) {
        if (initialized) throw new RuntimeException("Can't change envelope after initialization!");
        this.envelope_minX = x_min;
        this.envelope_maxX = x_max;
        this.envelope_minY = y_min;
        this.envelope_maxY = y_max;
        dilateEnvelope();
    };

    /**
     * WARNING: COULD ONLY BE USED BEFORE INITIALIZATION
     * @param x
     * @param y
     */
    public void expandEnvelopeToInclude( double x, double y ) {
        if (initialized) throw new RuntimeException("Can't change envelope after initialization!");

        if (!envelopeInitialized) {
            setEnvelope(x,x,y,y);
            return;
        }

        envelope_minX = Math.min(envelope_minX, x);
        envelope_maxX = Math.max(envelope_maxX, x);
        envelope_minY = Math.min(envelope_minY, y);
        envelope_maxY = Math.max(envelope_maxY, y);
        dilateEnvelope();
    }

    boolean envelopeInitialized = false;
    public boolean isEnvelopeInitialized() {
        return envelopeInitialized;
    }

    public void resetEnvelope() {
        envelopeInitialized = false;
    }

    public double getEnvelope_minX() {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        return envelope_minX;
    }

    public void setEnvelope_minX(double envelope_minX) {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        if (initialized) throw new RuntimeException("Can't change envelope after initialization!");
        this.envelope_minX = envelope_minX;
        dilateEnvelope();
    }

    public double getEnvelope_maxX() {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        return envelope_maxX;
    }

    public void setEnvelope_maxX(double envelope_maxX) {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        if (initialized) throw new RuntimeException("Can't change envelope after initialization!");
        this.envelope_maxX = envelope_maxX;
        dilateEnvelope();
    }

    public double getEnvelope_minY() {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        return envelope_minY;
    }

    public void setEnvelope_minY(double envelope_minY) {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        if (initialized) throw new RuntimeException("Can't change envelope after initialization!");
        this.envelope_minY = envelope_minY;
        dilateEnvelope();
    }

    public double getEnvelope_maxY() {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        return envelope_maxY;
    }

    public void setEnvelope_maxY(double envelope_maxY) {
        if (!envelopeInitialized) throw new RuntimeException("Envelope not initialized!");
        if (initialized) throw new RuntimeException("Can't change envelope after initialization!");
        this.envelope_maxY = envelope_maxY;
        dilateEnvelope();
    }

    private final double dilate_mult = 1.01;

    private void dilateEnvelope() {
        envelope_Dilated_minX = envelope_minX/dilate_mult;
        envelope_Dilated_maxX = envelope_maxX*dilate_mult;
        envelope_Dilated_minY = envelope_minY/dilate_mult;
        envelope_Dilated_maxY = envelope_maxY*dilate_mult;

        if (envelope_Dilated_minY == envelope_Dilated_maxY) {envelope_Dilated_minY-=Double.MIN_VALUE; envelope_Dilated_maxY+=Double.MIN_VALUE;};
        if (envelope_Dilated_minX == envelope_Dilated_maxX) {envelope_Dilated_minX-=Double.MIN_VALUE; envelope_Dilated_maxX+=Double.MIN_VALUE;};

        envelopeInitialized = true;
    }

    public void findInArea(Coordinate c, double r, Collection<T> ret) {
        findInArea(c.x,c.y,r,ret);
    }

    public void findInArea(double c_x, double c_y, double r, Collection<T> ret) {

        if (!initialized) throw new RuntimeException("Buffer not initialized!");

        int begin_x =   (int)GeomUtils.clamp(toLocalX(c_x-r),0,width-1);
        int end_x   =   (int)GeomUtils.clamp(toLocalX(c_x+r),0,width-1);
        int begin_y =   (int)GeomUtils.clamp(toLocalY(c_y-r),0,height-1);
        int end_y   =   (int)GeomUtils.clamp(toLocalY(c_y+r),0,height-1);

        for (int x = begin_x; x <= end_x; ++x) {
            for (int y = begin_y; y <= end_y; ++y) {
                getCell(x,y).forEach(ret::add);
            }
        }

    }

    public boolean hasInArea(double c_x, double c_y, double r) {
        if (!initialized) throw new RuntimeException("Buffer not initialized!");

        int begin_x =   (int)GeomUtils.clamp(toLocalX(c_x-r),0,width-1);
        int end_x   =   (int)GeomUtils.clamp(toLocalX(c_x+r),0,width-1);
        int begin_y =   (int)GeomUtils.clamp(toLocalY(c_y-r),0,height-1);
        int end_y   =   (int)GeomUtils.clamp(toLocalY(c_y+r),0,height-1);

        for (int x = begin_x; x <= end_x; ++x) {
            for (int y = begin_y; y <= end_y; ++y) {
                if (getCell(x,y).size() != 0) return true;
            }
        }
        return false;
    }

    public void getAll(Collection<T> ret) {

        if (!initialized) throw new RuntimeException("Buffer not initialized!");

        for (int y = 0; y != height; ++y) {
            for (int x = 0; x != width; ++x) {
                ret.addAll((cells[y * width + x]));
            }
        }

    }

}
