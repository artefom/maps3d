package ru.ogpscenter.maps3d.utils.fbx;

import com.vividsolutions.jts.geom.Coordinate;
import ru.ogpscenter.maps3d.algorithm.mesh.TexturedPatch;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class FBXConverter {

    private static class writeMeshRes{
        int model_id;
        int geometry_id;
        int last_free_index;
    }

    public static writeMeshRes writeMesh(
            Coordinate[] coordinates,
            Coordinate[] normals,
            Coordinate[] texture_coordinates,
            ArrayList<Integer> polygons,
            ArrayList<Integer> uv_ids,
            ArrayList<Integer> material_ids,
            String name, int index, FBXNode objects, FBXNode connections) {

        HashSet<Integer> unique_material_ids = new HashSet<>();
        for (Integer i : material_ids)
            unique_material_ids.add(i);

        writeMeshRes res = new writeMeshRes();
        res.model_id = index+2;
        res.geometry_id = index+1;
        res.last_free_index = index+3;

        FBXNode g = FBXDefaults.getDefaultGeometryDefinition(res.geometry_id);
        FBXNode m = FBXDefaults.getDefaultModelDefinition(res.model_id,name);

        g.subNodes.add(FBXDefaults.getVerticies(coordinates));
        g.subNodes.add(FBXDefaults.getPolygons(polygons));
        g.subNodes.add(FBXDefaults.getDefaultUVDefinition(texture_coordinates,uv_ids));
        g.subNodes.add(FBXDefaults.getDefaulLayerDefinition());
        g.subNodes.add(FBXDefaults.getLayerElementMaterial(material_ids));
        g.subNodes.add(FBXDefaults.getLayerElementSmoothing(material_ids.size()));
        g.subNodes.add(FBXDefaults.getLayerElementNormal(normals,polygons));

        {
            objects.subNodes.add(g);
            objects.subNodes.add(m);
            // Connect model to root
            connections.addSubNode("C","OO",m.properties.get(0),0);
            // Connect geometry to model
            connections.addSubNode("C","OO",g.properties.get(0),m.properties.get(0));
        }

        return res;
    }

    public static void serializeMesh(
            Coordinate[] coordinates,
            Coordinate[] normals,
            Coordinate[] texture_coordinates,
            ArrayList<Integer> polygons,
            ArrayList<Integer> uv_ids,
            ArrayList<Integer> material_ids,
            String path) throws FileNotFoundException {
        int object_count = 1;

        HashSet<Integer> unique_material_ids = new HashSet<>();
        for (Integer i : material_ids)
            unique_material_ids.add(i);

        FBXNode root = new FBXNode("");

        root.subNodes.add( FBXDefaults.getDefaultFbxHeader() );
        root.subNodes.add( FBXDefaults.getGlobalSettings() );
        root.subNodes.add( FBXDefaults.getDocumentsDescription() );
        root.subNodes.add( FBXDefaults.getObjectDefinitions( object_count, unique_material_ids.size() ) );

        FBXNode objects = new FBXNode("Objects");
        FBXNode connections = new FBXNode("Connections");


        writeMeshRes meshRes = writeMesh(coordinates,normals,texture_coordinates,polygons,uv_ids,material_ids,"dxGround",0,objects,connections);

        ArrayList<Integer> unique_materials_sorted = new ArrayList<>();

        unique_material_ids.forEach(unique_materials_sorted::add);
        unique_materials_sorted.sort(Integer::compare);

        for (Integer i : unique_materials_sorted)
            meshRes.last_free_index = writeMaterial(objects, connections,meshRes.model_id, i, meshRes.last_free_index);

        root.subNodes.add(objects);
        root.subNodes.add(connections);

        root.serialize(path);
    }

    private static Random mat_rand = new Random();
    private static int color_index = 0;
    private static double mat_r;
    private static double mat_g;
    private static double mat_b;
    private static void nextColor() {

        ++color_index;
        mat_r = (double)color_index/5;
        mat_g = (double)color_index/10;
        mat_b = (double)color_index/20;

        while (mat_r > 1) mat_r-=1;
        while (mat_g > 1) mat_g-=1;
        while (mat_b > 1) mat_b-=1;
    }

    public static int writeMaterial(FBXNode objects, FBXNode connections, int model_id, int material_id, int index) {

        nextColor();

        int material_index = index;
        ++index;
        int texture_index = index;
        ++index;
        String texture_caption = TexturedPatch.getDefaultTextureNameBase();
        String texture_local_path = TexturedPatch.extendTextureName(texture_caption,material_id);

        FBXNode mat_def = FBXDefaults.getDefaultMaterialDefinition(material_index,mat_r,mat_g,mat_b);
        FBXNode tex_def = FBXDefaults.getDefaultTextureDefinition(texture_index,texture_local_path);

        objects.subNodes.add(mat_def);
        objects.subNodes.add(tex_def);

        connections.addSubNode("C","OO",material_index,model_id);
        connections.addSubNode("C","OP",texture_index,material_index,"DiffuseColor");

        return index;
    }

//    public static void main(String[] args) throws FileNotFoundException {
////
////        int object_count = 1;
////
////        FBXNode root = new FBXNode("");
////
////        root.subNodes.add( FBXDefaults.getDefaultFbxHeader() );
////        root.subNodes.add( FBXDefaults.getGlobalSettings() );
////        root.subNodes.add( FBXDefaults.getDocumentsDescription() );
////        root.subNodes.add( FBXDefaults.getObjectDefinitions( object_count ) );
////
////        FBXNode objects = new FBXNode("Objects");
////        FBXNode connections = new FBXNode("Connections");
////
//
//        Mesh3D mesh = Mesh3D.fromPolygonsFBX(new double[] {
//            -0.5,-0.5,0,0.5,-0.5,0,-0.5,0.5,0,0.5,0.5,0,-0.5,-0.5,1,0.5,-0.5,1,-0.5,0.5,1,0.5,0.5,1
//        },new int[] {
//            0,2,3,-2,4,5,7,-7,0,1,5,-5,1,3,7,-6,3,2,6,-8,2,0,4,-7
//        });
//
//
//        mesh.saveAsFbx("Test.fbx");
//
//        //serializeMesh(mesh,"Test.fbx");
//
////        writeMesh(mesh,"Box01",0,objects,connections);
////
////        root.subNodes.add(objects);
////        root.subNodes.add(connections);
////
////        root.serialize("Test.fbx");
//
//    }

}
