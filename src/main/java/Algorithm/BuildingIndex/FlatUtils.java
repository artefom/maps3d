package Algorithm.BuildingIndex;

import com.vividsolutions.jts.geom.Triangle;
import com.vividsolutions.jts.triangulate.quadedge.TrianglePredicate;
import toxi.geom.AABB;
import toxi.geom.Triangle3D;
import toxi.geom.Vec3D;

/**
 * Created by fdl on 8/4/16.
 */
public class FlatUtils {
    public static Vec3D projectOnXZ(Vec3D vec3D){
        return new Vec3D(vec3D.getComponent(0), 0, vec3D.getComponent(2));
    }

    public static AABB projectOnXZ(AABB aabb){
        return new AABB(projectOnXZ(aabb.getMin()), projectOnXZ(aabb.getMax()));
    }

    public static Triangle3D projectOnXZ(Triangle3D tri){
        return new Triangle3D(FlatUtils.projectOnXZ(tri.a), FlatUtils.projectOnXZ(tri.b), FlatUtils.projectOnXZ(tri.c));
    }

    public static String toString(AABB aabb){ //TODO remove it
        return "min " + aabb.getMin().toString() + " max: " + aabb.getMax().toString();
    }

    public static String toString(Box box){ //TODO remove it
        return box.toString();
    }
}
