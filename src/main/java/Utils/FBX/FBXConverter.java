package Utils.FBX;

import Algorithm.Mesh.Mesh3D;
import com.vividsolutions.jts.geom.Coordinate;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class FBXConverter {

    public static int writeMesh(
            Coordinate[] coordinates,
            Coordinate[] normals,
            Coordinate[] texture_coordinates,
            ArrayList<Integer> polygons,
            ArrayList<Integer> uv_ids,
            ArrayList<Integer> material_ids,
            String name, int index, FBXNode objects, FBXNode connections) {

        FBXNode g = FBXDefaults.getDefaultGeometryDefinition(++index);
        FBXNode m = FBXDefaults.getDefaultModelDefinition(++index,name);

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
        return index;

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

        FBXNode root = new FBXNode("");

        root.subNodes.add( FBXDefaults.getDefaultFbxHeader() );
        root.subNodes.add( FBXDefaults.getGlobalSettings() );
        root.subNodes.add( FBXDefaults.getDocumentsDescription() );
        root.subNodes.add( FBXDefaults.getObjectDefinitions( object_count ) );

        FBXNode objects = new FBXNode("Objects");
        FBXNode connections = new FBXNode("Connections");


        writeMesh(coordinates,normals,texture_coordinates,polygons,uv_ids,material_ids,"GenericMesh",0,objects,connections);

        root.subNodes.add(objects);
        root.subNodes.add(connections);

        root.serialize(path);
    }

    public static void main(String[] args) throws FileNotFoundException {
//
//        int object_count = 1;
//
//        FBXNode root = new FBXNode("");
//
//        root.subNodes.add( FBXDefaults.getDefaultFbxHeader() );
//        root.subNodes.add( FBXDefaults.getGlobalSettings() );
//        root.subNodes.add( FBXDefaults.getDocumentsDescription() );
//        root.subNodes.add( FBXDefaults.getObjectDefinitions( object_count ) );
//
//        FBXNode objects = new FBXNode("Objects");
//        FBXNode connections = new FBXNode("Connections");
//

        Mesh3D mesh = Mesh3D.fromPolygonsFBX(new double[] {
            -0.5,-0.5,0,0.5,-0.5,0,-0.5,0.5,0,0.5,0.5,0,-0.5,-0.5,1,0.5,-0.5,1,-0.5,0.5,1,0.5,0.5,1
        },new int[] {
            0,2,3,-2,4,5,7,-7,0,1,5,-5,1,3,7,-6,3,2,6,-8,2,0,4,-7
        });


        mesh.saveAsFbx("Test.fbx");

        //serializeMesh(mesh,"Test.fbx");

//        writeMesh(mesh,"Box01",0,objects,connections);
//
//        root.subNodes.add(objects);
//        root.subNodes.add(connections);
//
//        root.serialize("Test.fbx");

    }

}