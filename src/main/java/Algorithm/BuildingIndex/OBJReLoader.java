package Algorithm.BuildingIndex;

import com.obj.Group;
import com.obj.WavefrontObject;
import toxi.geom.Vec3D;
import toxi.geom.mesh.WETriangleMesh;

/**
 * Created by fdl on 8/4/16.
 */
public class OBJReLoader {
    public static WETriangleMesh load(String filename){
        Group waveMesh = new WavefrontObject(filename).getGroups().get(0);
        WETriangleMesh toxiMesh = new WETriangleMesh();

        waveMesh.getFaces().forEach(face -> toxiMesh.addFace(
                        new Vec3D(face.getVertices()[0].getX(), face.getVertices()[0].getY(), face.getVertices()[0].getZ()),
                        new Vec3D(face.getVertices()[1].getX(), face.getVertices()[1].getY(), face.getVertices()[1].getZ()),
                        new Vec3D(face.getVertices()[2].getX(), face.getVertices()[2].getY(), face.getVertices()[2].getZ())
        ));

        System.out.println(waveMesh.getFaces().size() + " faces in waveMesh");
        System.out.println(toxiMesh.faces.size() + " faces in toxiMesh");

        return toxiMesh;
    }
}
