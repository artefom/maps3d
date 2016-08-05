package Algorithm.BuildingIndex;

import toxi.geom.Triangle3D;
import toxi.geom.Vec3D;
import toxi.geom.mesh.Mesh3D;
import toxi.geom.mesh.Vertex;

/**
 * Created by fdl on 8/5/16.
 */
public class Box {
    public final float x0, z0, x1, z1;

    private Box(float x0, float z0, float x1, float z1) {
        this.x0 = x0;
        this.x1 = x1;
        this.z0 = z0;
        this.z1 = z1;
        assert x0 <= x1 && z0 <= z1 : "new Box() contract check failed: " + this.toString();
    }

    public static Box createBox(Mesh3D mesh) {
        float x0 = Float.MAX_VALUE, x1 = Float.MIN_VALUE, z0 = Float.MAX_VALUE, z1 = Float.MIN_VALUE;
        for (Vertex v : mesh.getVertices()) {
            float tx = v.getComponent(0), tz = v.getComponent(2);
            x0 = Math.min(tx, x0);
            x1 = Math.max(tx, x1);
            z0 = Math.min(tz, z0);
            z1 = Math.max(tz, z1);
        }
        return new Box(x0, z0, x1, z1);
    }
    
//    public static Box safeBox(float x0, float x1, float z0, float z1){
//        return new Box(Math.m);
//    }
    
    public Box[] split(){
        float cx = (x0 + x1)/2, cz = (z0 + z1)/2;
        return new Box[]{
                new Box(cx, cz, x1, z1),
                new Box(x0, cz, cx, z1),
                new Box(x0, z0, cx, cz),
                new Box(cx, z0, x1, cz)
        };
    }

    private Vec3D tempVec = new Vec3D();
    private boolean testInT(float x, float z, Triangle3D t){
        tempVec.set(x, 0, z);
        return t.containsPoint(tempVec);
    }

    public boolean containsPoint(Vec3D v){
        float x = v.getComponent(0);
        float z = v.getComponent(2);
        return x0 <= x && x <= x1 && z0 <= z && z <= z1;
    }

    public boolean intersects(Triangle3D t){
        if (containsPoint(t.a)) return true;
        if (containsPoint(t.b)) return true;
        if (containsPoint(t.c)) return true;
        if (testInT(x0, z0, t)) return true;
        if (testInT(x0, z1, t)) return true;
        if (testInT(x1, z0, t)) return true;
        return testInT(x1, z1, t);
    }

    @Override
    public String toString(){
        return x0 + " " + z0 + " " + x1 + " " + z1;
    }
}
