package Algorithm.Mesh;

import Algorithm.Interpolation.DistanceFieldInterpolation;
import Algorithm.LineConnection.MapEdge;
import Algorithm.NearbyGraph.NearbyContainer;
import Algorithm.NearbyGraph.NearbyEstimator;
import Algorithm.NearbyGraph.NearbyGraphWrapper;
import Algorithm.Texture.PatchTextureGenerator;
import Deserialization.DeserializedOCAD;
import Isolines.IsolineContainer;
import Utils.*;
import Utils.Curves.CurveString;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.quadedge.Vertex;
import sun.security.krb5.internal.crypto.Des;

import java.awt.image.Raster;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Class, representing 3-dimentional mesh
 */
public class Mesh3D {

    private Mesh3D() {

    }

    ArrayList<Coordinate> vertexes;
    ArrayList<int[]> tris;
    Coordinate[] coord_array;

    double x_min;
    double x_max;
    double y_min;
    double y_max;
    double z_min;
    double z_max;

    public void buildMesh() {
        if (vertexes == null) throw new RuntimeException("First, initialize vertexes member");

        System.out.println("Triangulating");
        DelaunayTriangulationBuilder triangulator = new DelaunayTriangulationBuilder();
        triangulator.setSites(vertexes);

        GeometryFactory gf = new GeometryFactory();

        Geometry triangles = triangulator.getTriangles(gf);
        Envelope envelope = triangles.getEnvelopeInternal();

        // coordinates and their indexes
        int last_index = -1;
        // Use hashmap, so it's easy to find specific coordinate
        HashMap<Coordinate, Integer> coordinates = new HashMap<>();

        // sequences of coordinate indexes
        tris = new ArrayList<>();

        System.out.println("Converting to 3d mesh...");
        // Convert geometry to coordinates and triangles
        for (int i = 0; i != triangles.getNumGeometries(); ++i) {
            int[] tri = new int[3];
            LinearRing ls = (LinearRing) ((Polygon) triangles.getGeometryN(i)).getExteriorRing();
            for (int j = 0; j < 3 && j < ls.getNumPoints(); ++j) {
                Coordinate c = ls.getCoordinateN(j);
                if (!coordinates.containsKey(c)) {
                    coordinates.put(c, ++last_index);
                }
                int coordinate_index = coordinates.get(c);
                tri[j] = coordinate_index;
            }
            tris.add(tri);
        }

        // Convert hashmap to array
        coord_array = new Coordinate[last_index + 1];
        coordinates.forEach((c, i) -> coord_array[i] = c);

//        for (int i = 0; i != coord_array.length; ++i) {
//            // Invert y, so map not looks mirrored.
//            coord_array[i].y = envelope.getMaxY() - coord_array[i].y + envelope.getMinY();
//        }

    }

    private ArrayList<TexturedPatch> texturedPatches;

    public ArrayList<TexturedPatch> getTexturedPatches() {
        if (texturedPatches == null) {

            final long texture_pixel_count = 3000 * 3000;
            final long patch_texture_pixel_count = 2048 * 2048;
            final double max_area = ((x_max - x_min) * (y_max - y_min)) * ((double) patch_texture_pixel_count / texture_pixel_count);

            texturedPatches = generateTexturedPatches(-1, -1, max_area, -1, -1, true);
        }

        return texturedPatches;
    }

    public ArrayList<TexturedPatch> generateTexturedPatches(double max_width, double max_height,
                                                            double max_area, int max_triangle_count, int max_vertex_count, boolean mantain_aspect) {

        ArrayList<TexturedPatch> texturedPatches = new ArrayList<>();

        System.out.println("Calculating textured patches");

        TexturedPatch initial = TexturedPatch.fromTriangles(coord_array, tris);
        initial.setMantainAspect(true);
        initial.setEdgePadding(0.05);

        TexturedPatch.splitRecursivly(initial, texturedPatches, max_width, max_height, max_area, max_triangle_count, max_vertex_count, mantain_aspect);

        CommandLineUtils.report();

        return texturedPatches;
    }


    /**
     * Export mesh to obj format
     *
     * @param path
     */
    public void saveAsObj(String path) {

        System.out.println("Writing to file...");

        String extension = OutputUtils.getExtension(path);
        if (extension.length() == 0) path = path + ".obj";
        else {
            if (extension.compareTo("obj") != 0) throw new RuntimeException("Invalid extension");
        }
        PrintWriter out;
        try {
            out = new PrintWriter(path);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Could not save " + path);
        }

        double max_y = coord_array[0].y;
        // Write vertexes
        out.println("#Vertexes");
        for (int i = 0; i != coord_array.length; ++i) {
            Coordinate c = coord_array[i];
            double z = c.z;
            max_y = Math.max(max_y,c.y);
            out.println("v " + c.x + " " + z + " " + (-c.y));
        }

        out.println("#Texture coordinates");
        ArrayList<TexturedPatch> texturedPatches = getTexturedPatches();
        System.out.println("Number of texture patches: " + texturedPatches.size());
        ArrayList<Integer> texturedPatchesIndexOffset = new ArrayList<>();
        texturedPatchesIndexOffset.add(0);
        // Write texture coordinates
        int patch_id = 0;
        int texture_coordinate_offset = 0;
        for (TexturedPatch texturedPatch : texturedPatches) {


            ArrayList<Coordinate> textureCoordinates = texturedPatch.getTextureCoordinates();

            out.println("\n\n#Patch " + (patch_id));
            out.println("#Number of texture coordinates: " + textureCoordinates.size());
            out.println("#Offset: " + texture_coordinate_offset);

            texture_coordinate_offset += textureCoordinates.size();
            texturedPatchesIndexOffset.add(texture_coordinate_offset);

            for (Coordinate c : textureCoordinates) {

                out.println("vt " + (c.x + patch_id) + " " + (1-c.y) + " 0.0");
            }

            ++patch_id;

        }

        //Write faces
        out.println("#Triangle faces");
        out.println("g map");

        for (int i = 0; i != texturedPatches.size(); ++i) {
            TexturedPatch texturedPatch = texturedPatches.get(i);
            int textureCoordinateOffset = texturedPatchesIndexOffset.get(i);
            for (int[] tri : texturedPatch.triangles) {
                int v1_index = tri[0];
                int v2_index = tri[1];
                int v3_index = tri[2];
                int vt1_index = texturedPatch.getTextureCoordinateID(v1_index) + textureCoordinateOffset;
                int vt2_index = texturedPatch.getTextureCoordinateID(v2_index) + textureCoordinateOffset;
                int vt3_index = texturedPatch.getTextureCoordinateID(v3_index) + textureCoordinateOffset;
                out.println("f " + (v1_index + 1) + "/" + (vt1_index + 1) + " " +
                        (v2_index + 1) + "/" + (vt2_index + 1) + " " +
                        (v3_index + 1) + "/" + (vt3_index + 1));
            }
        }
//        for (int[] tri : tris) {
//            // Indexes in .obj format start from 1, so add 1 to indexes before writing
//            // Swap second and third indexes, so triangels will face up.
//            out.println("f "+(tri[0]+1)+" "+(tri[1]+1)+" "+(tri[2]+1));
//        }

        out.close();
    }

    public void generateTexture(DeserializedOCAD ocad, String out_path, String extension) {
        System.out.println("Generating texture");

        int patch_id = 0;
        for (TexturedPatch tp : getTexturedPatches()) {

            PatchTextureGenerator PTGen = new PatchTextureGenerator(ocad,tp);

            PTGen.writeToFile(out_path+"_"+patch_id+"_."+extension);

            patch_id += 1;

        }
    }


    public static Mesh3D fromHeightmap(double[] heightmap, int width, int height,
                                       Coordinate envelope_min, Coordinate envelope_max, int decimation_max_points, Polygon concaveHull) {
        double real_width = envelope_max.x - envelope_min.x;
        double real_height = envelope_max.y - envelope_min.y;
        double real_z_height = envelope_max.z - envelope_min.z;
        double crease_angle = 0.25;
        RasterUtils.map(heightmap,0,real_z_height);


        ArrayList<Coordinate> vertexes = new ArrayList<>();

        ArrayList<Coordinate> coords_buf = new ArrayList<>();
        //* Row curve strings *//

        float[] importantPoints = new float[heightmap.length];

        CommandLineUtils.reportProgressBegin("Gathering vertexes");
        for (int row = 0; row != height; ++row) {
            CommandLineUtils.reportProgress(row,height+width);
            coords_buf.clear();
            for (int column = 0; column != width; ++column) {

                double x = GeomUtils.map(column,0,width-1,envelope_min.x,envelope_max.x)+0.5;
                double z = heightmap[row*width+column];
                coords_buf.add(new Coordinate(x,z));
            }

            CurveString curveString = CurveString.fromCoordinatesLinear(coords_buf);
            coords_buf.clear();
            curveString.interpolate(coords_buf,crease_angle);
            for (Coordinate c : coords_buf) {
                int column = (int)GeomUtils.map( c.x, envelope_min.x, envelope_max.x, 0, width);
                if (column >= width) column = width-1;
                importantPoints[row*width+column] = 1;
            }
        }

        for (int column = 0; column != width; ++column) {
            CommandLineUtils.reportProgress(column+height,height+width);
            coords_buf.clear();
            for (int row = 0; row != height; ++row) {
                double y = GeomUtils.map(row,0,height-1,envelope_min.y,envelope_max.y)+0.5;
                double z = heightmap[row*width+column];
                coords_buf.add(new Coordinate(z,y));
            }

            CurveString curveString = CurveString.fromCoordinatesLinear(coords_buf);
            coords_buf.clear();
            curveString.interpolate(coords_buf,crease_angle);

            for (Coordinate c : coords_buf) {
                int row = (int)GeomUtils.map( c.y, envelope_min.y, envelope_max.y, 0, height);
                if (row >= height) row = height-1;
                importantPoints[row*width+column] = 1;
            }

        }
        CommandLineUtils.reportProgressEnd();

//        if (decimation_max_points > 0) {
//
////            float[] importantPoints_mask = Arrays.copyOf(importantPoints,importantPoints.length);
////            RasterUtils.gauss(importantPoints_mask,width,height,2,3);
////
////            for (int i = 0; i != importantPoints.length; ++i) {
////                importantPoints[i]*=importantPoints_mask[i];
////            }
//
//            //PointScatter sc = new PointScatter();
//
//            //sc.setDistribution(importantPoints, width, height);
//            //sc.setMaxPointCount(decimation_max_points);
//            //sc.setEnvelope(envelope_min.x, envelope_min.y, envelope_max.x, envelope_max.y);
//            //sc.setMaxRadius(Math.max(envelope_max.x - envelope_min.x, envelope_max.y - envelope_min.y) / 20);
//            //sc.scatterPoints(vertexes);
//
//            for (Coordinate c : vertexes) {
//                int row = (int) (GeomUtils.map(c.y, envelope_min.y, envelope_max.y, 1, height - 1) + 0.5);
//                int column = (int) (GeomUtils.map(c.x, envelope_min.x, envelope_max.x, 1, width - 1) + 0.5);
//                c.z = heightmap[row * width + column];
//            }
//
//        } else {
        for (int i = 0; i != importantPoints.length; ++i) {
            if (importantPoints[i] > 0.5) {
                int row = i/width;
                int column = i%width;
                double x = GeomUtils.map(column,0,width-1,envelope_min.x,envelope_max.x);
                double y = GeomUtils.map(row,0,height-1,envelope_min.y,envelope_max.y);
                double z = heightmap[row*width+column];
                vertexes.add(new Coordinate(x,y,z));
            }
        }
       // }

        if (concaveHull != null) {

            Envelope chull_envelope = concaveHull.getEnvelopeInternal();

            double c_min_x = chull_envelope.getMinX();
            double c_max_x = chull_envelope.getMaxX();
            double c_min_y = chull_envelope.getMinY();
            double c_max_y = chull_envelope.getMaxY();
            Coordinate[] chull_coords = concaveHull.getExteriorRing().getCoordinates();
            for (int i = 0; i < chull_coords.length-1; ++i) {
                chull_coords[i].x = GeomUtils.map(chull_coords[i].x,c_min_x,c_max_x,envelope_min.x,envelope_max.x);
                chull_coords[i].y = GeomUtils.map(chull_coords[i].y,c_min_y,c_max_y,envelope_min.y,envelope_max.y);
            }
            concaveHull = concaveHull.getFactory().createPolygon(chull_coords);


            ArrayList<Coordinate> processedVertexes = new ArrayList<>();
            GeometryFactory gf = concaveHull.getFactory();
            for (Coordinate c : vertexes) {
                Point p = gf.createPoint(c);

                if (concaveHull.contains(p)) {
                    processedVertexes.add(c);
                }
            }
            LineString ls = concaveHull.getExteriorRing();
            LineStringInterpolatedPointIterator it = new LineStringInterpolatedPointIterator(ls,ls.getLength()/500,0);
            while (it.hasNext()) {
                Coordinate c = it.next();
                int row = (int)(GeomUtils.map(c.y, envelope_min.y, envelope_max.y, 1, height - 1) + 0.5);
                int column = (int)(GeomUtils.map(c.x, envelope_min.x, envelope_max.x, 1, width - 1) + 0.5);
                if (row == height) row = height-1;
                if (column == width) column = width -1;
                if (column < 0 || column >= width || row < 0 || row >= height) continue;
                c.z = heightmap[row*width+column];
                processedVertexes.add(c);
            }
            vertexes = processedVertexes;
        }

        return Mesh3D.fromVertexes(vertexes);
    }

    public static Mesh3D fromHeightmap_old(float[] heightmap, int width, int height, Coordinate envelope_min, Coordinate envelope_max) {
        double real_width = envelope_max.x - envelope_min.x;
        double real_height = envelope_max.y - envelope_min.y;
        double real_z_height = envelope_max.z - envelope_min.z;

        ArrayList<Coordinate> vertexes = new ArrayList<>();
        PointScatter sc = new PointScatter();

        RasterUtils.map(heightmap,0,real_z_height);

        float[] normals = new float[heightmap.length];
        float[] angles = new float[heightmap.length];

        RasterUtils.angleSobelBased(heightmap,normals,angles,width,height,real_width/width,real_height/height);

        for (int i = 0; i != normals.length; ++i) {
            angles[i] = (float)(angles[i]/Math.PI);
        }

//        RasterUtils.dilate(sobel1,width,height);
//        RasterUtils.erode(sobel1,width,height);
//        RasterUtils.erode(sobel1,width,height);
//        RasterUtils.dilate(sobel1,width,height);
        //RasterUtils.gauss(sobel1,width,height,1,1);

        RasterUtils.save(angles,width,height,"mesh_heightmap"+debug_prefix+"_angles.png");
        RasterUtils.saveAsTxt(angles,width,height,"mesh_heightmap"+debug_prefix+"_angles");
        RasterUtils.save(normals,width,height,"mesh_heightmap"+debug_prefix+"_normals.png");
        RasterUtils.saveAsTxt(normals,width,height,"mesh_heightmap"+debug_prefix+"_normals");

//        for (int i = 0; i != normals.length; ++i) {
//            angles[i] = (float)( (angles[i]/Math.PI+normals[i]/real_z_height)*0.5 );
//        }

//        RasterUtils.dilate(sobel2,width,height);
//        RasterUtils.erode(sobel2,width,height);
//        RasterUtils.erode(sobel2,width,height);
//        RasterUtils.dilate(sobel2,width,height);
//        RasterUtils.gauss(sobel2,width,height,3,3);


        float[] angles_sobel = RasterUtils.sobel(angles, width, height);
        float[] normal_sobel = RasterUtils.sobel(normals, width, height);
        float[] sobel2_fine = angles_sobel;

        for (int i = 0; i != angles_sobel.length; ++i) {
            angles_sobel[i]*=normals[i];
            angles_sobel[i] = (float)GeomUtils.clamp( GeomUtils.map(angles_sobel[i],0,3,0,1), 0,1);
            normal_sobel[i] = (float)GeomUtils.clamp( GeomUtils.map(normal_sobel[i],0,6,0,1), 0,1.4);
            //normal_sobel[i] = normal_sobel[i]*normal_sobel[i];
            sobel2_fine[i] = Math.max(angles_sobel[i],normal_sobel[i]);
            //#Math.max(normal_sobel[i],angles_sobel[i]);
        }

        int blur_size = (int)Math.ceil((double)Math.min(width,height)/200);
        RasterUtils.bloom(angles_sobel,width,height,0.1,blur_size);

        CommandLineUtils.reportProgressBegin("Dumping files");
        RasterUtils.save(angles_sobel,width,height,0,1,"mesh_heightmap"+debug_prefix+"_angles_sobel.png");
        CommandLineUtils.reportProgress(1,4);
        RasterUtils.saveAsTxt(angles_sobel,width,height,"mesh_heightmap"+debug_prefix+"_angles_sobel");
        CommandLineUtils.reportProgress(2,4);
        RasterUtils.save(normal_sobel,width,height,0,1,"mesh_heightmap"+debug_prefix+"_normals_sobel.png");
        CommandLineUtils.reportProgress(3,4);
        RasterUtils.saveAsTxt(normal_sobel,width,height,"mesh_heightmap"+debug_prefix+"_normals_sobel");
        CommandLineUtils.reportProgressEnd();

        sc.setDistribution(sobel2_fine,width,height);
        sc.setMaxPointCount(6);
        sc.setEnvelope(envelope_min.x,envelope_min.y,envelope_max.x,envelope_max.y);
        sc.setMaxRadius(Math.max(envelope_max.x-envelope_min.x,envelope_max.y-envelope_min.y)/20);
        sc.scatterPoints(vertexes);

        double row_scale = height/real_height;
        double column_scale = width/real_width;

        ArrayList<Coordinate> valid_vertexes = new ArrayList<>();
        for (Coordinate c : vertexes) {

            int row = GeomUtils.clamp((int)GeomUtils.map(c.y,envelope_min.y,envelope_max.y,0,height),0,height-1);
            int column = GeomUtils.clamp((int)GeomUtils.map(c.x,envelope_min.x,envelope_max.x,0,width),0,width-1);
            double r = sc.getRadius( sobel2_fine[row*width+column] )*1.2;
            double start_x = c.x-r;
            double start_y = c.y-r;
            double end_x = c.x+r;
            double end_y = c.y+r;

            int start_column = GeomUtils.clamp((int)GeomUtils.map(start_x,envelope_min.x,envelope_max.x,0,width),0,width-1);
            int start_row    = GeomUtils.clamp((int)GeomUtils.map(start_y,envelope_min.y,envelope_max.y,0,height),0,height-1);
            int end_column   = GeomUtils.clamp((int)GeomUtils.map(end_x,envelope_min.x,envelope_max.x,0,width),0,width-1)+1;
            int end_row      = GeomUtils.clamp((int)GeomUtils.map(end_y,envelope_min.y,envelope_max.y,0,height),0,height-1)+1;

            double z = 0;
            int count = (end_column-start_column)*(end_row-start_row);
            if (count == 0) continue;
            for (row = start_row; row != end_row; ++row) {
                for (column = start_column; column != end_column; ++column) {
                    z += heightmap[row*width+column];
                }
            }
            c.z = z/count;
            valid_vertexes.add(c);
        }

        return Mesh3D.fromVertexes(vertexes);
    }


    public static Mesh3D fromVertexes(Collection<Coordinate> vertexes) {

        Iterator<Coordinate> it = vertexes.iterator();
        if (!it.hasNext()) return null;
        Coordinate c = it.next();
        double x_min = c.x;
        double x_max = c.x;
        double y_min = c.y;
        double y_max = c.y;
        double z_min = c.z;
        double z_max = c.z;

        while (it.hasNext()) {
            c = it.next();

            x_min = Math.min(x_min,c.x);
            x_max = Math.max(x_max,c.x);

            y_min = Math.min(y_min,c.y);
            y_max = Math.max(y_max,c.y);

            z_min = Math.min(z_min,c.z);
            z_max = Math.max(z_max,c.z);
        }



        Mesh3D mesh = new Mesh3D();

        mesh.x_min = x_min;
        mesh.x_max = x_max;
        mesh.y_min = y_min;
        mesh.y_max = y_max;
        mesh.z_min = z_min;
        mesh.z_max = z_max;
        mesh.vertexes = new ArrayList<>();

        vertexes.forEach(mesh.vertexes::add);

        mesh.buildMesh();

        return mesh;
    }

    public double getWidth() {return x_max-x_min;}
    public double getHeight() {return y_max-y_min;}
    public double getZHeight() {return z_max-z_min;}

    public void saveVertexesAsImage(String path) {

        int pixel_count = 3000000; //3 megapixels
        double real_width = getWidth();
        double real_height = getHeight();

        int width = (int)(Math.sqrt(real_width*pixel_count/real_height));
        int height = (int)(Math.sqrt(real_height*pixel_count/real_width));

        double[] img = new double[width*height];

        for (Coordinate c : vertexes) {
            int row = (int) (GeomUtils.map(c.y, y_min, y_max, 1, height - 1) + 0.5);
            int column = (int) (GeomUtils.map(c.x,x_min, x_max, 1, width - 1) + 0.5);
            if (row < 0 || row >= height || column < 0 || column >= width) {
                continue;
            }

            img[row*width+column] += 1;
        }

        RasterUtils.save(img,width,height,path);
    }

    public static String debug_prefix = "";
//    public static void processheightmap(float[] heightmap, int width, int height, String outputPostfix, Polygon concaveHull) {
//
//
//        RasterUtils.padding(heightmap,width,height,2);
//
//        debug_prefix = outputPostfix;
//
//        double real_width = 1;
//        double real_height = (float)height/width;
//        double real_z_height = 0.2;
//
//        double multiplier = 10;
//
//        real_height*=multiplier;
//        real_width*=multiplier;
//        real_z_height*=multiplier;
//
//        Coordinate c_min = new Coordinate(-real_width*0.5,-real_height*0.5,0);
//        Coordinate c_max = new Coordinate(real_width*0.5,real_height*0.5,real_z_height);
//
//        Mesh3D mesh = Mesh3D.fromHeightmap(heightmap,width,height,c_min,c_max);
//
//        System.out.println("Dumping vertexes as image");
//        mesh.saveVertexesAsImage("mesh_vertexes"+outputPostfix+".png");
//
//        System.out.println("Dumping mesh");
//        mesh.saveAsObj("mesh"+outputPostfix);
//        CommandLineUtils.report();
//    }

    public static void processheightmap(double[] heightmap, Coordinate c_min, Coordinate c_max, int width, int height, String outputPostfix, Polygon concaveHull) throws Exception {


        RasterUtils.padding(heightmap,width,height,2);

        debug_prefix = outputPostfix;

        double real_width = 1;
        double real_height = (float)height/width;
        double real_z_height = 0.06;

        double multiplier = 50;

        real_height*=multiplier;
        real_width*=multiplier;
        real_z_height*=multiplier;

        Mesh3D mesh = Mesh3D.fromHeightmap(heightmap,width,height,c_min,c_max,8000000,concaveHull);

        System.out.println("Dumping vertexes as image");
        mesh.saveVertexesAsImage("mesh_vertexes"+outputPostfix+".png");

        System.out.println("Deserializing ocad...");


        System.out.println("Dumping mesh");

        mesh.saveAsObj("mesh"+outputPostfix);

        DeserializedOCAD ocad = new DeserializedOCAD();
        ocad.DeserializeMap("sample.ocd",null);
        mesh.generateTexture(ocad,"mesh_texture"+outputPostfix,"png");
        CommandLineUtils.report();
    }

//
//    public static void processImageHeightmap(String path, String outputPostfix) {
//        Pair<Integer,float[]> ret_pair = RasterUtils.loadARGBasGrayscaleFloat(path);
//
//        int width = ret_pair.v1;
//        int height = ret_pair.v2.length/width;
//        float[] heightmap = ret_pair.v2;
//
//        processheightmap(heightmap,width,height,outputPostfix,null);
//    }
//
//    public static void processTextHeightmap(String path,String outputPostfix) {
//        Pair<Integer,float[]> ret_pair = RasterUtils.loadTextAsGrayscaleFloat(path);
//
//        int width = ret_pair.v1;
//        int height = ret_pair.v2.length/width;
//        float[] heightmap = ret_pair.v2;
//
//        processheightmap(heightmap,width,height,outputPostfix,null);
//    }

    public static void main(String[] args) throws Exception {

        IsolineContainer ic = IsolineContainer.deserialize("sample_clean_map2.json");
        NearbyContainer cont = new NearbyContainer(ic);
        NearbyEstimator est = new NearbyEstimator(ic.getFactory());
        NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));
        graph.SetHillsSlopeSides();
        graph.ConvertToSpanningTree();
        graph.recoverAllSlopes();
        graph.recoverAllHeights();

        DistanceFieldInterpolation interpolation = new DistanceFieldInterpolation(ic);
        double[][] heightmap_matrix = interpolation.getAllInterpolatingPoints();
        int width = heightmap_matrix[0].length;
        int height = heightmap_matrix.length;
        double[] heightmap = RasterUtils.linearize( heightmap_matrix);
        Polygon map_area = MapEdge.getConvexHull(ic);

        //Coordinate c_min = new Coordinate(-real_width*0.5,-real_height*0.5,0);
        //Coordinate c_max = new Coordinate(real_width*0.5,real_height*0.5,real_z_height);

        PointRasterizer rast = interpolation.getRasterizer();

        double height_multiplier = 15;
        double map_z_height = (interpolation.getMaxHeight()-interpolation.getMinHeight())*height_multiplier;

        Coordinate c_min = new Coordinate(rast.toX(0),      rast.toY(0),        0);
        Coordinate c_max = new Coordinate(rast.toX(width),  rast.toY(height),   map_z_height);

        processheightmap(heightmap,c_min,c_max,width,height,"_normal",map_area);

//        processTextHeightmap("sample.txt","_normal");
//        processImageHeightmap("sample_small.png","_small");
//        processImageHeightmap("sample_tiny.png","_tiny");
    }
}
