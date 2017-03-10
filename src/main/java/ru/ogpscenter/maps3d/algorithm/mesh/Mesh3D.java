package ru.ogpscenter.maps3d.algorithm.mesh;

import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector3D;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import ru.ogpscenter.maps3d.algorithm.Texture.PatchTextureGenerator;
import ru.ogpscenter.maps3d.algorithm.index.BaseMesh;
import ru.ogpscenter.maps3d.algorithm.interpolation.DistanceFieldInterpolation;
import ru.ogpscenter.maps3d.algorithm.repair.MapEdge;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;
import ru.ogpscenter.maps3d.utils.*;
import ru.ogpscenter.maps3d.utils.area.PolygonAreaBuffer;
import ru.ogpscenter.maps3d.utils.curves.CurveString;
import ru.ogpscenter.maps3d.utils.fbx.FBXConverter;
import ru.ogpscenter.maps3d.utils.properties.PropertiesLoader;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class, representing 3-dimentional mesh
 */
public class Mesh3D {

    private HashMap<TOcadObject, Geometry> geometryCache =  new HashMap<>();

    private Mesh3D() {

    }

    public ArrayList<Coordinate> vertexes;
    public ArrayList<int[]> tris;

    public Coordinate[] coordinatesArray;
    public Coordinate[] normals;

    public double x_min;
    public double x_max;
    public double y_min;
    public double y_max;
    public double z_min;
    public double z_max;

    public void buildMeshFromVertices() {
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

        coordinatesArray = new Coordinate[last_index + 1];
        coordinates.forEach((c, i) -> coordinatesArray[i] = c);
        calculateNormals();
        initializePolygonBuffer();
    }


    public static Coordinate[] calculateNormals(Coordinate[] coord_array,ArrayList<int[]> polygons) {
        Coordinate[] normals = new Coordinate[coord_array.length];
        for (int i = 0; i != normals.length; ++i) {
            normals[i] = new Coordinate(0,0,0);
        }


        for (int[] polygon : polygons) {
            for (int i = 0; i != polygon.length; ++i) {

                int i2 = polygon[i];
                int i1 = i == 0 ? polygon[polygon.length-1] : polygon[i-1];
                int i3 = i == polygon.length-1 ? polygon[0] : polygon[i+1];

                Vector3D vec1 = (new Vector3D(coord_array[i1],coord_array[i2])).normalize();
                Vector3D vec2 = (new Vector3D(coord_array[i2],coord_array[i3])).normalize();

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
        }

        for (int i = 0; i != normals.length; ++i) {
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

        return normals;
    }

    PolygonAreaBuffer poly_buf;
    public void initializePolygonBuffer() {
        poly_buf = new PolygonAreaBuffer(coordinatesArray,tris,200,200);
    }

    public void calculateNormals() {
        normals = calculateNormals(coordinatesArray,tris);
    }


    private ArrayList<TexturedPatch> texturedPatches;

    public ArrayList<TexturedPatch> getTexturedPatches() {
        if (texturedPatches == null) {

            final double area = ((x_max - x_min) * (y_max - y_min));

//            final double max_area = ((x_max - x_min) * (y_max - y_min)) * ((double) patch_texture_pixel_count / texture_pixel_count);

            texturedPatches = generateTexturedPatches(-1, -1, area*PropertiesLoader.textured_patch.max_area, -1,
                    PropertiesLoader.textured_patch.max_vertices, PropertiesLoader.textured_patch.preserve_aspect);
        }

        return texturedPatches;
    }

    public ArrayList<TexturedPatch> generateTexturedPatches(double max_width, double max_height,
                                                            double max_area, int max_triangle_count, int max_vertex_count, boolean mantain_aspect) {

        ArrayList<TexturedPatch> texturedPatches = new ArrayList<>();

        System.out.println("Calculating textured patches");

        TexturedPatch initial = TexturedPatch.fromTriangles(coordinatesArray, tris);
        initial.setMantainAspect(mantain_aspect);
        initial.setEdgePadding(PropertiesLoader.textured_patch.padding);

        TexturedPatch.splitRecursivly(initial, texturedPatches, max_width, max_height, max_area, max_triangle_count, max_vertex_count, mantain_aspect);

        CommandLineUtils.reportFinish();

        return texturedPatches;
    }

    public static Coordinate[] transformCoordinates(Coordinate[] coords) {

        //Copy array
        Coordinate[] vertices = new Coordinate[coords.length];
        for (int i = 0; i != vertices.length; ++i) {
            vertices[i] = new Coordinate(coords[i]);
        }

        double x_offset = 0;
        double y_offset = 0;
        double z_offset = vertices[0].z;

        // calculate z_offset
        for (int i = 0; i != vertices.length; ++i) {
            if (z_offset > vertices[i].z) z_offset = vertices[i].z;
        }
        z_offset = -z_offset+PropertiesLoader.mesh_output.z_offset;

        if (PropertiesLoader.mesh_output.zero_centered) {

            double min_x = vertices[0].x;
            double max_x = vertices[0].x;
            double min_y = vertices[0].y;
            double max_y = vertices[0].y;

            for (int i = 0; i != vertices.length; ++i) {
                if (min_x > vertices[i].x) min_x = vertices[i].x;
                if (min_y > vertices[i].y) min_y = vertices[i].y;
                if (max_x < vertices[i].x) max_x = vertices[i].x;
                if (max_y < vertices[i].y) max_y = vertices[i].y;
            }

            x_offset = -(min_x+max_x)*0.5;
            y_offset = -(min_y+max_y)*0.5;
        }

        for (int i = 0; i != vertices.length; ++i) {

            vertices[i].x += x_offset;
            vertices[i].y += y_offset;
            vertices[i].z += z_offset;

            vertices[i].x *= PropertiesLoader.mesh_output.scale;
            vertices[i].y *= PropertiesLoader.mesh_output.scale;
            vertices[i].z *= PropertiesLoader.mesh_output.scale;
            vertices[i].z *= PropertiesLoader.mesh_output.z_scale;

        }

        return vertices;
    }

    public BaseMesh saveAsFbx(String path) {

        System.out.println("Dumping fbx");

        ArrayList<Coordinate> tex_coord_array = new ArrayList<>();

        ArrayList<Integer> material_ids = new ArrayList<>();

        ArrayList<TexturedPatch> texturedPatches = getTexturedPatches();
        System.out.println("Number of texture patches: " + texturedPatches.size());
        ArrayList<Integer> texturedPatchesIndexOffset = new ArrayList<>();
        texturedPatchesIndexOffset.add(0);
        // Write texture coordinates
        int texture_coordinate_offset = 0;

        for (TexturedPatch texturedPatch : texturedPatches) {

            ArrayList<Coordinate> textureCoordinates = texturedPatch.getTextureCoordinates();

            texture_coordinate_offset += textureCoordinates.size();
            texturedPatchesIndexOffset.add(texture_coordinate_offset);

            tex_coord_array.addAll(textureCoordinates.stream().map(c -> new Coordinate(c.x, 1 - c.y)).collect(Collectors.toList()));
        }

        ArrayList<Integer> polygon_indexes = new ArrayList<>();
        ArrayList<Integer> texture_indexes = new ArrayList<>();

        for (int i = 0; i != texturedPatches.size(); ++i) {
            TexturedPatch texturedPatch = texturedPatches.get(i);
            int textureCoordinateOffset = texturedPatchesIndexOffset.get(i);
            for (int[] tri : texturedPatch.getTriangles()) {
                for (int tri_i = 0; tri_i != tri.length; ++tri_i) {
                    int v_index = tri[tri_i];
                    int vt_index = texturedPatch.getTextureCoordinateID(v_index) + textureCoordinateOffset;
                    if (tri_i == tri.length-1) {
                        polygon_indexes.add(-1*(v_index+1));
                    } else {
                        polygon_indexes.add(v_index);
                    }
                    texture_indexes.add(vt_index);
                }
                material_ids.add(i);
            }
        }

        Coordinate[] texture_coordinates = tex_coord_array.toArray(new Coordinate[tex_coord_array.size()]);

        Coordinate[] vertices = transformCoordinates(coordinatesArray);
        Coordinate[] normals = calculateNormals(vertices,tris);

        try {
            FBXConverter.serializeMesh(vertices,normals,texture_coordinates,polygon_indexes,texture_indexes,material_ids, path+".fbx");
            Optional<Coordinate> minZ = Arrays.stream(vertices).min(Comparator.comparingDouble(o -> o.z));
            Optional<Coordinate> maxZ = Arrays.stream(vertices).max(Comparator.comparingDouble(o -> o.z));
            if (minZ.isPresent() && maxZ.isPresent()) {
                System.out.println("Z coordinate range: [" + minZ.get().z + " - " + maxZ.get().z + "]");
            }
        } catch (Exception ignored) {

            CommandLineUtils.reportException(ignored);

        };

        return new BaseMesh(vertices, tris);
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

        double max_y = coordinatesArray[0].y;
        // Write vertexes
        out.println("#Vertexes");
        for (int i = 0; i != coordinatesArray.length; ++i) {
            Coordinate c = coordinatesArray[i];
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
            for (int[] tri : texturedPatch.getTriangles()) {
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

        out.close();
    }

    public void generateTexture(DeserializedOCAD ocad, String outPath, String extension) {
        System.out.println("Generating texture");
        long startTime = System.currentTimeMillis();
        // Load brushes from vmt files
        List<PatchTextureGenerator.Brush> brushes = PatchTextureGenerator.loadBrushes(PatchTextureGenerator.getTextureFolder());
        geometryCache.clear();
        int patch_id = 0;
        for (TexturedPatch texturedPatch : getTexturedPatches()) {
            PatchTextureGenerator patchTextureGenerator = new PatchTextureGenerator(ocad, texturedPatch, this, brushes, geometryCache);
            String fullPath = TexturedPatch.extendTextureName(outPath + "." + extension,patch_id);
            CommandLineUtils.reportProgressBegin("Patch #" + patch_id);
            patchTextureGenerator.generateAndWriteToFile(fullPath);
            patch_id += 1;
        }
        System.out.println("Done in " + (System.currentTimeMillis() - startTime) + " ms.");
    }


    public static Mesh3D fromHeightmap(double[] heightmap, int width, int height,
                                       Coordinate envelope_min, Coordinate envelope_max, Polygon concaveHull) {
        double real_width = envelope_max.x - envelope_min.x;
        double real_height = envelope_max.y - envelope_min.y;
        double real_z_height = envelope_max.z - envelope_min.z;
        double crease_angle = PropertiesLoader.mesh_creation.crease_angle;
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

        mesh.buildMeshFromVertices();

        return mesh;
    }

    public static Mesh3D fromPolygonsFBX(double[] coordinates, int[] polygons) {


        Mesh3D mesh = new Mesh3D();
        mesh.vertexes = new ArrayList<>();
        for (int i = 0; i < coordinates.length-2; i+=3) {
            mesh.vertexes.add(new Coordinate(coordinates[i],coordinates[i+1],coordinates[i+2]));
        }

        mesh.x_min = coordinates[0];
        mesh.x_max = coordinates[0];
        mesh.y_min = coordinates[1];
        mesh.y_max = coordinates[1];
        mesh.z_min = coordinates[2];
        mesh.z_max = coordinates[2];

        for (int i = 0; i < coordinates.length-2; i+=3) {;

            mesh.x_min = Math.min(mesh.x_min,coordinates[i]);
            mesh.x_max = Math.max(mesh.x_max,coordinates[i]);

            mesh.y_min = Math.min(mesh.y_min,coordinates[i+1]);
            mesh.y_max = Math.max(mesh.y_max,coordinates[i+1]);

            mesh.z_min = Math.min(mesh.z_min,coordinates[i+2]);
            mesh.z_max = Math.max(mesh.z_max,coordinates[i+2]);
        }

        mesh.coordinatesArray = new Coordinate[mesh.vertexes.size()];
        for (int i = 0; i != mesh.coordinatesArray.length; ++i) {
            mesh.coordinatesArray[i] = mesh.vertexes.get(i);
        }
        mesh.tris = new ArrayList<>();

        int begin = 0;
        int end = begin;
        do {
            while ( end < polygons.length-1 && polygons[end] >= 0 ) {
                ++end;
            }
            ++end;

            int[] poly = new int[end-begin];
            for (int i = begin; i != end; ++i) {
                if (polygons[i] < 0) poly[i-begin] = -polygons[i]-1;
                else poly[i-begin] = polygons[i];
            }
            mesh.tris.add(poly);
            begin = end;
        } while (end < polygons.length);

        mesh.calculateNormals();
        mesh.initializePolygonBuffer();

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

    public static Mesh3D fromIsolineContainer(IsolineContainer ic) {
        PropertiesLoader.update();

        DistanceFieldInterpolation interpolation = new DistanceFieldInterpolation(ic);
        double[][] heightmap_matrix = interpolation.getAllInterpolatingPoints();
        int width = heightmap_matrix[0].length;
        int height = heightmap_matrix.length;
        double[] heightmap = RasterUtils.linearize( heightmap_matrix);

        //Coordinate c_min = new Coordinate(-real_width*0.5,-real_height*0.5,0);
        //Coordinate c_max = new Coordinate(real_width*0.5,real_height*0.5,real_z_height);

        PointRasterizer rast = interpolation.getRasterizer();

        double height_multiplier = PropertiesLoader.mesh_creation.isoline_height_delta;
        double map_z_height = (interpolation.getMaxHeight()-interpolation.getMinHeight())*height_multiplier;

        Coordinate c_min = new Coordinate(rast.toX(0),      rast.toY(0),        0);
        Coordinate c_max = new Coordinate(rast.toX(width),  rast.toY(height),   map_z_height);

        RasterUtils.padding(heightmap,width,height,PropertiesLoader.mesh_creation.heightmap_padding);

        Polygon map_area = MapEdge.getConvexHull(ic);

        if (PropertiesLoader.mesh_creation.convex_hull_cull) {
            return Mesh3D.fromHeightmap(heightmap, width, height, c_min, c_max, map_area);
        } else {
            return Mesh3D.fromHeightmap(heightmap, width, height, c_min, c_max, null);
        }

    }

    private static Coordinate point_height_buf = new Coordinate();
    public double getPointHeight(double x, double y) {

        point_height_buf.x = x;
        point_height_buf.y = y;

        int[] triangle = poly_buf.getPolygonByPoint(x,y);
        if (triangle == null) return Double.NaN;
        //return coordinatesArray[triangle[0]].z;

        double area_0 = GeomUtils.area(point_height_buf, coordinatesArray[triangle[1]], coordinatesArray[triangle[2]]);
        double area_1 = GeomUtils.area(point_height_buf, coordinatesArray[triangle[0]], coordinatesArray[triangle[2]]);
        double area_2 = GeomUtils.area(point_height_buf, coordinatesArray[triangle[1]], coordinatesArray[triangle[0]]);
        double area_sum = area_0+area_1+area_2;

        return (coordinatesArray[triangle[0]].z*area_0+ coordinatesArray[triangle[1]].z*area_1+ coordinatesArray[triangle[2]].z*area_2)/area_sum;
    }

    Vector3D z_normal = new Vector3D(0,0,1);
    public double getPointAngle(double x, double y) {

        point_height_buf.x = x;
        point_height_buf.y = y;

        int[] triangle = poly_buf.getPolygonByPoint(x,y);
        if (triangle == null) return Double.NaN;
        //return coordinatesArray[triangle[0]].z;

        double area_0 = GeomUtils.area(point_height_buf, coordinatesArray[triangle[1]], coordinatesArray[triangle[2]]);
        double area_1 = GeomUtils.area(point_height_buf, coordinatesArray[triangle[0]], coordinatesArray[triangle[2]]);
        double area_2 = GeomUtils.area(point_height_buf, coordinatesArray[triangle[1]], coordinatesArray[triangle[0]]);
        double area_sum = area_0+area_1+area_2;

        Coordinate angle = new Coordinate(0,0,0);
        angle.x = (normals[triangle[0]].x*area_0+normals[triangle[1]].x*area_1+normals[triangle[2]].x*area_2)/area_sum;
        angle.y = (normals[triangle[0]].y*area_0+normals[triangle[1]].y*area_1+normals[triangle[2]].y*area_2)/area_sum;
        angle.z = (normals[triangle[0]].z*area_0+normals[triangle[1]].z*area_1+normals[triangle[2]].z*area_2)/area_sum;
        Vector3D norm = new Vector3D(angle);
        norm.normalize();
        return Math.abs( Math.acos( norm.dot(z_normal) ) )/Math.PI*2;
    }

    public void splitTexture(BufferedImage image, DeserializedOCAD ocad, String textureOutputPath, String extension) throws IOException {
        System.out.println("Splitting texture");
        int patch_id = 0;
        for (TexturedPatch texturedPatch : getTexturedPatches()) {
            PatchTextureGenerator patchTextureGenerator = new PatchTextureGenerator(ocad, texturedPatch, this, Collections.emptyList(), geometryCache);
            String fullPath = TexturedPatch.extendTextureName(textureOutputPath + "." + extension, patch_id);
            patchTextureGenerator.splitAndWriteToFile(image, fullPath);
            patch_id += 1;
        }
        System.out.println("{ \"textures\" : [");
        for (int i = 0; i < getTexturedPatches().size(); i++) {
            String fullPath = TexturedPatch.extendTextureName(textureOutputPath + "." + extension, i);
            System.out.print((i != 0 ? "," : "") + "\n\t\"" + CoSpacesBase62Id.calculateIdFromFile(fullPath) + "\"");
        }
        System.out.println("]}");
    }
}
