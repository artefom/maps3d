package Algorithm.BuildingIndex;

import Algorithm.Interpolation.Triangulation;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import java.util.Arrays;

/**
 * Created by fdl on 8/9/16.
 */
public class BaseMesh extends Mesh {
    public BaseMesh(String objFileName) {
        super(objFileName);
    }

    BaseMesh(Triangulation triangulation){
        Arrays.asList(triangulation.getMeshVertexes()).forEach(vertexXZY -> {
            vertexesXZ.add(vertexXZY);
            vertexesY.add(vertexXZY.z);
            boxXZ.update(vertexXZY.x, vertexXZY.z);
        });
        triangulation.getTrianglesIndices().forEach(tri -> {
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

    @Override
    public void move(double dx, double dy, double dz) {
        throw new RuntimeException("Can't move BaseMesh, cause QTree is already assigned to it");
    }
}
