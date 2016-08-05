package Algorithm.BuildingIndex;

import toxi.geom.Triangle3D;
import toxi.geom.mesh.Face;

/**
 * Created by fdl on 8/4/16.
 */
class WrappedTriangle {
    final Triangle3D triFlat;
    final Face face;
    final int hash;

    public WrappedTriangle(Face face) {
        triFlat = FlatUtils.projectOnXZ(face.toTriangle());
        this.face = face;
        hash = triFlat.a.hashCode() ^ triFlat.b.hashCode() ^ triFlat.c.hashCode();//TODO
    }
}
