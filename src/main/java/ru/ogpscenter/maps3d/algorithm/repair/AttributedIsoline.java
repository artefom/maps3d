package ru.ogpscenter.maps3d.algorithm.repair;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.Isoline;

/**
 * Wrapper around {@link IIsoline}, containing all sufficient information about {@link LineEnd}s.
 */
public class AttributedIsoline {

    private IIsoline iso;
    public LineEnd begin;
    public LineEnd end;
    private Geometry geometry;

    public AttributedIsoline() {
        iso = null;
        begin = null;
        end = null;
    }

    public AttributedIsoline(IIsoline iso) {
        this.iso = iso;
        if (iso.getLineString().isClosed()) {
            this.begin = null;
            this.end = null;
            return;
        }
        LineSegment begin_seg = LineEnd.getEnd(this.iso.getLineString(),1);
        LineSegment end_seg = LineEnd.getEnd(this.iso.getLineString(),-1);
        this.begin = new LineEnd(this,begin_seg,1);
        this.end = new LineEnd(this,end_seg,-1);
        this.begin.other = this.end;
        this.end.other = this.begin;
    }

    /**
     * Initialize with line segments (does not perform copy), internal use only
     * @param iso
     * @param begin
     * @param end
     */
    public AttributedIsoline(IIsoline iso, LineEnd begin, LineEnd end) {
        this.iso = iso;
        this.begin = begin;
        this.end = end;

        if (this.begin.isoline == this.end.isoline) {
            this.begin.isoline.invalidate();
            this.begin = null;
            this.end = null;
            return;
        }

        if (this.begin.isoline != null)
            this.begin.isoline.invalidate();
        if (this.end.isoline != null)
            this.end.isoline.invalidate();

        this.begin.isoline = this;
        this.begin.end_index = 1;
        this.end.isoline = this;
        this.end.end_index = -1;
        this.begin.other = this.end;
        this.end.other = this.begin;
    }

    public boolean isClosed() {
        return iso.getLineString().isClosed();
    }

    public Geometry getGeometry() {
        return iso.getGeometry();
    }

    public LineString getLineString() {
        return iso.getLineString();
    }

    public int getSlopeSide() {
        return iso.getSlopeSide();
    }

    public int getType() {
        return iso.getType();
    }

    @Override
    public int hashCode() {
        return iso.hashCode();
    }

    public boolean isEdgeToEdge(MapEdge edge) {
        if (begin == null || end == null) return false;
        return this.begin.isWithinEdge(edge) && this.end.isWithinEdge(edge);
    }

    @Override
    public boolean equals(Object obj) {
        if (!iso.equals(obj)) return false;
        AttributedIsoline other = (AttributedIsoline)obj;
        if (!begin.equals(other.begin) || !end.equals(other.end)) return false;
        return true;
    }

    @Override
    public String toString() {
        return iso.toString();
    }

    public IIsoline getIsoline() {
        return new Isoline(iso);
    }

    public GeometryFactory getFactory() {
        return iso.getFactory();
    }

    public void invalidate() {
        iso = null;
        if (begin.isoline == this)
            begin.isoline = null;
            begin.other = null;
        if (end.isoline == this)
            end.isoline = null;
            end.other = null;
        begin = null;
        end = null;
    }

    public boolean isValid() {
        if (iso != null && (
                (begin != null && end != null && begin.isoline == this &&
                end.isoline == this && begin.other == end && end.other == begin) ||
                (begin == null && end == null))) {
            return true;
        }
        return false;
    }
}
