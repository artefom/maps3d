package Utils.FBX;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.math.Vector3D;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Artyom.Fomenko on 03.09.2016.
 */
public class FBXDefaults {


    public static FBXNode getFbxNodePropertyTemplate() {

        FBXNode propertyTemplate = new FBXNode("PropertyTemplate");
        propertyTemplate.properties.add("FbxNode");
        FBXNode p = new FBXNode("Properties70");
        propertyTemplate.subNodes.add(p);

        p.setP("QuaternionInterpolate","enum","","",0);
        p.setP("RotationOffset","Vector3D","","Vector",0,0,0);
        p.setP("RotationPivot","Vector3D","","Vector",0,0,0);
        p.setP("ScalingOffset","Vector3D","","Vector",0,0,0);
        p.setP("ScalingPivot","Vector3D","","Vector",0,0,0);
        p.setP("TranslationActive","bool","","",0);
        p.setP("TranslationMin","Vector3D","","Vector",0,0,0);
        p.setP("TranslationMax","Vector3D","","Vector",0,0,0);
        p.setP("TranslationMinX","bool","","",0);
        p.setP("TranslationMinY","bool","","",0);
        p.setP("TranslationMinZ","bool","","",0);
        p.setP("TranslationMaxX","bool","","",0);
        p.setP("TranslationMaxY","bool","","",0);
        p.setP("TranslationMaxZ","bool","","",0);
        p.setP("RotationOrder","enum","","",0);
        p.setP("RotationSpaceForLimitOnly","bool","","",0);
        p.setP("RotationStiffnessX","double","","Number",0);
        p.setP("RotationStiffnessY","double","","Number",0);
        p.setP("RotationStiffnessZ","double","","Number",0);
        p.setP("AxisLen","double","","Number",10);
        p.setP("PreRotation","Vector3D","","Vector",0,0,0);
        p.setP("PostRotation","Vector3D","","Vector",0,0,0);
        p.setP("RotationActive","bool","","",0);
        p.setP("RotationMin","Vector3D","","Vector",0,0,0);
        p.setP("RotationMax","Vector3D","","Vector",0,0,0);
        p.setP("RotationMinX","bool","","",0);
        p.setP("RotationMinY","bool","","",0);
        p.setP("RotationMinZ","bool","","",0);
        p.setP("RotationMaxX","bool","","",0);
        p.setP("RotationMaxY","bool","","",0);
        p.setP("RotationMaxZ","bool","","",0);
        p.setP("InheritType","enum","","",0);
        p.setP("ScalingActive","bool","","",0);
        p.setP("ScalingMin","Vector3D","","Vector",0,0,0);
        p.setP("ScalingMax","Vector3D","","Vector",1,1,1);
        p.setP("ScalingMinX","bool","","",0);
        p.setP("ScalingMinY","bool","","",0);
        p.setP("ScalingMinZ","bool","","",0);
        p.setP("ScalingMaxX","bool","","",0);
        p.setP("ScalingMaxY","bool","","",0);
        p.setP("ScalingMaxZ","bool","","",0);
        p.setP("GeometricTranslation","Vector3D","","Vector",0,0,0);
        p.setP("GeometricRotation","Vector3D","","Vector",0,0,0);
        p.setP("GeometricScaling","Vector3D","","Vector",1,1,1);
        p.setP("MinDampRangeX","double","","Number",0);
        p.setP("MinDampRangeY","double","","Number",0);
        p.setP("MinDampRangeZ","double","","Number",0);
        p.setP("MaxDampRangeX","double","","Number",0);
        p.setP("MaxDampRangeY","double","","Number",0);
        p.setP("MaxDampRangeZ","double","","Number",0);
        p.setP("MinDampStrengthX","double","","Number",0);
        p.setP("MinDampStrengthY","double","","Number",0);
        p.setP("MinDampStrengthZ","double","","Number",0);
        p.setP("MaxDampStrengthX","double","","Number",0);
        p.setP("MaxDampStrengthY","double","","Number",0);
        p.setP("MaxDampStrengthZ","double","","Number",0);
        p.setP("PreferedAngleX","double","","Number",0);
        p.setP("PreferedAngleY","double","","Number",0);
        p.setP("PreferedAngleZ","double","","Number",0);
        p.setP("LookAtProperty","object","","");
        p.setP("UpVectorProperty","object","","");
        p.setP("Show","bool","","",1);
        p.setP("NegativePercentShapeSupport","bool","","",1);
        p.setP("DefaultAttributeIndex","int","","Integer",-1);
        p.setP("Freeze","bool","","",0);
        p.setP("LODBox","bool","","",0);
        p.setP("Lcl Translation","Lcl Translation","A","",0,0,0);
        p.setP("Lcl Rotation","Lcl Rotation","A","",0,0,0);
        p.setP("Lcl Scaling","Lcl Scaling","A","",1,1,1);
        p.setP("Visibility","Visibility","A","",1);
        p.setP("Visibility Inheritance","Visibility Inheritance","","",1);

        return propertyTemplate;
    }

    public static FBXNode getFbxMeshPropertyTemplate() {
        FBXNode propertyTemplate = new FBXNode("PropertyTemplate");
        propertyTemplate.properties.add("FbxMesh");
        FBXNode p = new FBXNode("Properties70");
        propertyTemplate.subNodes.add(p);

        p.setP("Color", "ColorRGB", "Color", "",0.8,0.8,0.8);
        p.setP("BBoxMin", "Vector3D", "Vector", "",0,0,0);
        p.setP("BBoxMax", "Vector3D", "Vector", "",0,0,0);
        p.setP("Primary Visibility", "bool", "", "",1);
        p.setP("Casts Shadows", "bool", "", "",1);
        p.setP("Receive Shadows", "bool", "", "",1);

        return propertyTemplate;
    }

    public static FBXNode getDefaultFbxHeader() {

        FBXNode header = new FBXNode("FBXHeaderExtension");

        header.addSubNode("FBXHeaderVersion",1003);
        header.addSubNode("FBXVersion",7300);

        {
            FBXNode cts = new FBXNode("CreationTimeStamp");
            cts.addSubNode("Version", 1000);
            cts.addSubNode("Year", 0);
            cts.addSubNode("Month", 0);
            cts.addSubNode("Day", 0);
            cts.addSubNode("Hour", 0);
            cts.addSubNode("Minute", 0);
            cts.addSubNode("Second", 0);
            cts.addSubNode("Millisecond", 0);
            header.subNodes.add(cts);
        }

        header.addSubNode("Creator","Me");

        {
            FBXNode si = new FBXNode("SceneInfo");
            si.properties.add("SceneInfo::GlobalInfo");
            si.properties.add("UserData");
            si.addSubNode("Type","UserData");
            si.addSubNode("Version",100);
            {
                FBXNode md = new FBXNode("MetaData");
                md.addSubNode("Version",100);
                md.addSubNode("Title","");
                md.addSubNode("Subject","");
                md.addSubNode("Author","");
                md.addSubNode("Keywords","");
                md.addSubNode("Revision","");
                md.addSubNode("Comment","");
                si.subNodes.add(md);
            }

            {
                FBXNode p = new FBXNode("Properties70");

                p.setP("DocumentUrl", "KString", "Url", "", "");
                p.setP("SrcDocumentUrl", "KString", "Url", "", "");
                p.setP("Original", "Compound", "", "");
                p.setP("Original|ApplicationVendor", "KString", "", "", "");
                p.setP("Original|ApplicationName", "KString", "", "", "");
                p.setP("Original|ApplicationVersion", "KString", "", "", "");
                p.setP("Original|DateTime_GMT", "DateTime", "", "", "");
                p.setP("Original|FileName", "KString", "", "", "");
                p.setP("LastSaved", "Compound", "", "");
                p.setP("LastSaved|ApplicationVendor", "KString", "", "", "");
                p.setP("LastSaved|ApplicationName", "KString", "", "", "");
                p.setP("LastSaved|ApplicationVersion", "KString", "", "", "");
                p.setP("LastSaved|DateTime_GMT", "DateTime", "", "", "");
                p.setP("Original|ApplicationActiveProject", "KString", "", "", "");

                si.subNodes.add(p);
            }

            header.subNodes.add(si);
        }

        return header;
    }

    public static FBXNode getGlobalSettings() {
        FBXNode gs = new FBXNode("GlobalSettings");
        gs.addSubNode("Version",1000);
        FBXNode p = new FBXNode("Properties70");


        p.setP("UpAxis", "int", "Integer", "",1);
        p.setP("UpAxisSign", "int", "Integer", "",1);
        p.setP("FrontAxis", "int", "Integer", "",2);
        p.setP("FrontAxisSign", "int", "Integer", "",1);
        p.setP("CoordAxis", "int", "Integer", "",0);
        p.setP("CoordAxisSign", "int", "Integer", "",1);
        p.setP("OriginalUpAxis", "int", "Integer", "",2);
        p.setP("OriginalUpAxisSign", "int", "Integer", "",1);
        p.setP("UnitScaleFactor", "double", "Number", "",2.54);
        p.setP("OriginalUnitScaleFactor", "double", "Number", "",2.54);
        p.setP("AmbientColor", "ColorRGB", "Color", "",0,0,0);
        p.setP("DefaultCamera", "KString", "", "", "Producer Perspective");
        p.setP("TimeMode", "enum", "", "",6);
        p.setP("TimeProtocol", "enum", "", "",2);
        p.setP("SnapOnFrameMode", "enum", "", "",0);
        p.setP("TimeSpanStart", "KTime", "Time", "",0);
        p.setP("TimeSpanStop", "KTime", "Time", "", 153953860000L);
        p.setP("CustomFrameRate", "double", "Number", "",-1);
        p.setP("TimeMarker", "Compound", "", "");
        p.setP("CurrentTimeMarker", "int", "Integer", "",-1);
        gs.subNodes.add(p);
        return gs;
    }

    public static FBXNode getDocumentsDescription() {
        FBXNode docs = new FBXNode("Documents");
        docs.addSubNode("Count",1);

        {
            FBXNode doc = new FBXNode("Document");
            doc.properties.add(1455729264);
            doc.properties.add("");
            doc.properties.add("Scene");
            {
                FBXNode p = new FBXNode("Properties70");
                p.setP("SourceObject", "object", "", "");
                p.setP("ActiveAnimStackName", "KString", "", "", "");
                doc.subNodes.add(p);
            }
            doc.addSubNode("RootNode",0);
            docs.subNodes.add(doc);
        }

        return docs;
    }

    public static FBXNode getObjectDefinitions(int object_count) {

        FBXNode od = new FBXNode("Definitions");
        od.addSubNode("Version", 100);
        od.addSubNode("Count", 7);

        {
            FBXNode otp = new FBXNode("ObjectType");
            otp.properties.add("GlobalSettings");
            otp.addSubNode("Count",1);
            od.subNodes.add(otp);
        }

        {
            FBXNode mdl = new FBXNode("ObjectType");
            mdl.properties.add("Model");
            mdl.addSubNode("Count",object_count);
            mdl.subNodes.add(getFbxNodePropertyTemplate());
            od.subNodes.add(mdl);
        }

        {
            FBXNode g = new FBXNode("ObjectType");
            g.properties.add("Geometry");
            g.addSubNode("Count",object_count);
            g.subNodes.add(getFbxMeshPropertyTemplate());
            od.subNodes.add(g);
        }

        return od;

    }

    public static int getGeometryVersion() {
        return 124;
    }

    public static int getModelVersion() {
        return 232;
    }

    public static FBXNode getGeometryDefaultProperties() {

        FBXNode p = new FBXNode("Properties70");
        p.setP("Color", "ColorRGB", "Color", "",0.109803921568627,0.109803921568627,0.694117647058824);
        return p;

    }

    public static FBXNode getModelDefaultProperties() {

        FBXNode p = new FBXNode("Properties70");

        p.setP("PreRotation", "Vector3D", "Vector", "",-90,0,0);
        p.setP("RotationActive", "bool", "", "",1);
        p.setP("InheritType", "enum", "", "",1);
        p.setP("ScalingMax", "Vector3D", "Vector", "",0,0,0);
        p.setP("DefaultAttributeIndex", "int", "Integer", "",0);
        p.setP("Lcl Translation", "Lcl Translation", "", "A",0,0,0);
        p.setP("mr displacement use global settings", "Bool", "", "AU",1);
        p.setP("mr displacement view dependent", "Bool", "", "AU",1);
        p.setP("mr displacement method", "Integer", "", "AU",6,6,6);
        p.setP("mr displacement smoothing on", "Bool", "", "AU",1);
        p.setP("mr displacement edge length", "Number", "", "AU",2,2,2);
        p.setP("mr displacement max displace", "Number", "", "AU",20,20,20);
        p.setP("mr displacement parametric subdivision level", "Integer", "", "AU",5,5,5);
        p.setP("MaxHandle", "int", "Integer", "UH",3);

        return p;
    }

    public static FBXNode getDefaultModelDefinition(int index, String name) {
        FBXNode m = new FBXNode("Model");
        m.properties.add(index);
        m.properties.add("Model::"+name);
        m.properties.add("Mesh");
        m.addSubNode("Version",getModelVersion());
        m.subNodes.add(getModelDefaultProperties());
        m.addSubNode("Shading",FBXNode.ValidSymbols.T);
        m.addSubNode("Culling", "CullingOff");
        return m;
    }

    public static FBXNode getDefaultGeometryDefinition(int index) {
        FBXNode g = new FBXNode("Geometry");
        g.properties.add(index);
        g.properties.add("Geometry::");
        g.properties.add("Mesh");
        g.subNodes.add(getGeometryDefaultProperties());
        g.addSubNode("GeometryVersion",getGeometryVersion());

        return g;
    }

    public static FBXNode getDefaultUVDefinition(Coordinate[] texture_coordinates, ArrayList<Integer> indexes) {

        ArrayList<Double> tex_coords = new ArrayList<>();
        ArrayList<Integer> coord_indexes = new ArrayList<>();

        for (int i = 0; i != texture_coordinates.length; ++i) {
            tex_coords.add(texture_coordinates[i].x);
            tex_coords.add(texture_coordinates[i].y);
        }

        for (Integer ind : indexes) {
            coord_indexes.add(ind);
        }

        return getDefaultUVDefinition(tex_coords,coord_indexes);
    }

    public static FBXNode getDefaultUVDefinition(Collection<Double> uvCoordinates,Collection<Integer> indexes) {

        FBXNode le = new FBXNode("LayerElementUV");
        le.properties.add(0);
        le.addSubNode("Version",101);
        le.addSubNode("UVChannel_1");
        le.addSubNode("MappingInformationType","ByPolygonVertex");
        le.addSubNode("ReferenceInformationType","IndexToDirect");

        FBXNode uvs = new FBXNode("UV");
        uvs.properties.add(new FBXNode.Ammount(uvCoordinates.size()));
        FBXNode uvs_a = new FBXNode("a");
        for (Double d : uvCoordinates) {
            uvs_a.properties.add(d);
        }
        uvs.subNodes.add(uvs_a);

        FBXNode uvi = new FBXNode("UVIndex");
        uvi.properties.add(new FBXNode.Ammount(indexes.size()));
        FBXNode uvi_a = new FBXNode("a");
        for (Integer i : indexes) {
            uvi_a.properties.add(i);
        }
        uvi.subNodes.add(uvi_a);

        le.subNodes.add(uvs);
        le.subNodes.add(uvi);

        return le;
    }

    public static FBXNode getLayerElementSmoothing(int size) {

        FBXNode ls = new FBXNode("LayerElementSmoothing");
        ls.properties.add(0);
        ls.addSubNode("Version",102);
        ls.addSubNode("Name","");
        ls.addSubNode("MappingInformationType","ByPolygon");
        ls.addSubNode("ReferenceInformationType","Direct");

        FBXNode s = new FBXNode("Smoothing");
        s.properties.add(new FBXNode.Ammount(size));

        FBXNode s_a = new FBXNode("a");
        for (int i = 0; i != size; ++i) {
            s_a.properties.add(0);
        }

        s.subNodes.add(s_a);
        ls.subNodes.add(s);

        return ls;
    }

    public static FBXNode getLayerElementMaterial(ArrayList<Integer> material_ids) {

        FBXNode le = new FBXNode("LayerElementMaterial");
        le.properties.add(0);
        le.addSubNode("Version",0);
        le.addSubNode("Name","");
        le.addSubNode("MappingInformationType","ByPolygon");
        le.addSubNode("ReferenceInformationType","IndexToDirect");

        FBXNode mats = new FBXNode("Materials");
        mats.properties.add(new FBXNode.Ammount(material_ids.size()));
        FBXNode mats_a = new FBXNode("a");
        for (Integer i : material_ids) {
            mats_a.properties.add(i);
        }
        mats.subNodes.add(mats_a);
        le.subNodes.add(mats);

        return le;
    }


    public static FBXNode getLayerElementNormal(Coordinate[] coordinates, ArrayList<Integer> polygons) {

        Coordinate[] normals = new Coordinate[coordinates.length];
        for (int i = 0; i != normals.length; ++i) {
            normals[i] = new Coordinate(0,0,0);
        }

        int begin = 0;
        int end = begin;

        while (end < polygons.size()) {

            while (end < polygons.size()-1 && polygons.get(end) >= 0) end+=1;
            end += 1;

            for (int i = begin; i < end; ++i) {

                int i1;
                int i2 = polygons.get(i);
                int i3;
                if (i <= begin) {
                    i1 = polygons.get(end-1);
                } else {
                    i1 = polygons.get(i-1);
                }
                if (i >= end-1) {
                    i3 = polygons.get(begin);
                } else {
                    i3 = polygons.get(i+1);
                }

                if (i1 < 0) i1 = -i1-1;
                if (i2 < 0) i2 = -i2-1;
                if (i3 < 0) i3 = -i3-1;

                Vector3D vec1 = (new Vector3D(coordinates[i1],coordinates[i2])).normalize();
                Vector3D vec2 = (new Vector3D(coordinates[i2],coordinates[i3])).normalize();

                double u1 = vec1.getX();
                double u2 = vec1.getY();
                double u3 = vec1.getZ();
                double v1 = vec2.getX();
                double v2 = vec2.getY();
                double v3 = vec2.getZ();

                double uvi, uvj, uvk;
                uvi = u2 * v3 - v2 * u3;
                uvj = v1 * u3 - u1 * v3;
                uvk = u1 * v2 - v1 * u2;

                normals[i2].x += uvi;
                normals[i2].y += uvj;
                normals[i2].z += uvk;
            }

            begin = end;
        }
//        for (int i = 0; i < polygons.size()-2; ++i) {
//            int i1 = polygons.get(i);
//            int i2 = polygons.get(i+1);
//            int i3 = polygons.get(i+2);
//            if (i1 < 0 || i2 < 0) continue;
//            if (i3 < 0) i3 = -i3-1;
//
//            Vector3D vec1 = new Vector3D(coordinates[i1],coordinates[i2]);
//            Vector3D vec2 = new Vector3D(coordinates[i2],coordinates[i3]);
//
//            double u1 = vec1.getX();
//            double u2 = vec1.getY();
//            double u3 = vec1.getZ();
//            double v1 = vec2.getX();
//            double v2 = vec2.getY();
//            double v3 = vec2.getZ();
//
//            double uvi, uvj, uvk;
//            uvi = u2 * v3 - v2 * u3;
//            uvj = v1 * u3 - u1 * v3;
//            uvk = u1 * v2 - v1 * u2;
//
//            normals[i2].x += uvi;
//            normals[i2].y += uvj;
//            normals[i2].z += uvk;
//        }

        for (int i = 0; i != coordinates.length; ++i) {
            double x = normals[i].x;
            double y = normals[i].y;
            double z = normals[i].z;
            double length = Math.sqrt(x*x+y*y+z*z);
            x/=length;
            y/=length;
            z/=length;
            normals[i].x = x;
            normals[i].y = y;
            normals[i].z = z;
        }

        ArrayList<Double> byPolygonVertex = new ArrayList<>();

        for (Integer i : polygons) {
            if ( i < 0 ) i = -i - 1;
            byPolygonVertex.add(normals[i].x);
            byPolygonVertex.add(normals[i].y);
            byPolygonVertex.add(normals[i].z);
        }

        FBXNode ln = new FBXNode("LayerElementNormal");
        ln.properties.add(0);
        ln.addSubNode("Version",101);
        ln.addSubNode("Name","");
        ln.addSubNode("MappingInformationType","ByPolygonVertex");
        ln.addSubNode("ReferenceInformationType","Direct");

        FBXNode n = new FBXNode("Normals");
        n.properties.add(new FBXNode.Ammount(byPolygonVertex.size()));

        FBXNode n_a = new FBXNode("a");
        for (Double d : byPolygonVertex) {
            n_a.properties.add(d);
        }

        n.subNodes.add(n_a);
        ln.subNodes.add(n);

        return ln;
    }

//    LayerElementNormal: 0 {
//        Version: 101
//        Name: ""
//        MappingInformationType: "ByPolygonVertex"
//        ReferenceInformationType: "Direct"
//        Normals: *192 {

    public static FBXNode getDefaulLayerDefinition() {
        FBXNode l = new FBXNode("Layer");
        l.properties.add(0);
        l.addSubNode("Version",100);

        {
            FBXNode le = new FBXNode("LayerElement");
            le.addSubNode("Type","LayerElementNormal");
            le.addSubNode("TypedIndex",0);
            l.subNodes.add(le);
        }

        {
            FBXNode le = new FBXNode("LayerElement");
            le.addSubNode("Type","LayerElementMaterial");
            le.addSubNode("TypedIndex",0);
            l.subNodes.add(le);
        }

        {
            FBXNode le = new FBXNode("LayerElement");
            le.addSubNode("Type","LayerElementSmoothing");
            le.addSubNode("TypedIndex",0);
            l.subNodes.add(le);
        }

        {
            FBXNode le = new FBXNode("LayerElement");
            le.addSubNode("Type","LayerElementUV");
            le.addSubNode("TypedIndex",0);
            l.subNodes.add(le);
        }

        return l;
    }
}
