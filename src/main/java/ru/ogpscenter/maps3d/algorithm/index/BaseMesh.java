package ru.ogpscenter.maps3d.algorithm.index;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import ru.ogpscenter.maps3d.algorithm.interpolation.Triangulation;
import ru.ogpscenter.maps3d.algorithm.mesh.Mesh3D;

import java.util.Arrays;
import java.util.List;

/**
 * Created by fdl on 8/9/16.
 */
public class BaseMesh extends Mesh {
    public BaseMesh(String objFileName) {
        super(objFileName);
    }

    public BaseMesh(Coordinate [] coord_array, List <int[]> tris){
        Arrays.asList(coord_array).forEach(vertexXZY -> {
            vertexesXZ.add(vertexXZY);
            vertexesY.add(vertexXZY.z);
            boxXZ.update(vertexXZY.x, vertexXZY.z);
        });
        tris.forEach(tri -> {
            Triplet face = new Triplet(tri);
            faceIndices.add(face);
            Polygon polygon = gf.createPolygon(new Coordinate[]{
                    vertexesXZ.get(face.a),
                    vertexesXZ.get(face.b),
                    vertexesXZ.get(face.c),
                    vertexesXZ.get(face.a)
            });
            polygon.setUserData(trianglesXZ.size());
            trianglesXZ.add(polygon);
        });
        boxXZ.acceptUpdates();
        initialVertexesCount = vertexesXZ.size();
        getInitialFacesCount = faceIndices.size();

    }

    BaseMesh(Triangulation triangulation){
        this(triangulation.getMeshVertexes(), triangulation.getTrianglesIndices());
    }

    public BaseMesh(Mesh3D arteMesh) {
        this(arteMesh.coord_array, arteMesh.tris);
    }

    @Override
    public void move(double dx, double dy, double dz) {
        throw new RuntimeException("Can't move BaseMesh, cause QTree is already assigned to it");
    }
}
