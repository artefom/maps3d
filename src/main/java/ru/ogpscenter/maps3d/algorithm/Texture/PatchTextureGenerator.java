package ru.ogpscenter.maps3d.algorithm.Texture;

import Deserialization.Binary.OcadVertex;
import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import Deserialization.Interpolation.SlopeMark;
import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;
import org.jetbrains.annotations.Nullable;
import ru.ogpscenter.maps3d.algorithm.mesh.Mesh3D;
import ru.ogpscenter.maps3d.algorithm.mesh.TexturedPatch;
import ru.ogpscenter.maps3d.utils.*;
import ru.ogpscenter.maps3d.utils.curves.CurveString;
import ru.ogpscenter.maps3d.utils.properties.PropertiesLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * generates texture for deserializedOCAD
 */
public class PatchTextureGenerator {

    private final HashMap<TOcadObject, Geometry> geometryCache;
    DeserializedOCAD deserializedOCAD;
    TexturedPatch patch;
    Mesh3D mesh;
    private List<PatchTextureGenerator.Brush> brushes;

    public PatchTextureGenerator(DeserializedOCAD deserializedOCAD, TexturedPatch texturedPatch,
                                 Mesh3D mesh, List<Brush> brushes, HashMap<TOcadObject, Geometry> geometryCache) {
        this.deserializedOCAD = deserializedOCAD;
        this.patch = texturedPatch;
        this.mesh = mesh;
        this.brushes = brushes;
        this.geometryCache = geometryCache;
    }

    public class Brush {
        public String name = "Unknown";
        public String symbol_ids = "";
        public boolean filled;
        public String color;
        public int z_index = -1;
        public int mask_blur = 0;
        public String z_mask_bezier_definitions = "";
        public String angle_mask_bezier_definitions = "";
        public String texture;
        public float width = 1;

        public boolean erode_first;
        public int dilate_size;
        public int erode_size;
        public int blur_size;

        public String blend_mode = "";
        private BufferedImage textureImage;

        public boolean shouldFill() {
            return filled;
        }

        public boolean isOverlay() {
            return symbol_ids == null || symbol_ids.length() == 0;
        }

        public String getName( ){
            return name;
        }

        public int getColor()  {
            short fill_r = 0;
            short fill_g = 0;
            short fill_b = 0;
            short fill_a = 255;
            String[] color_values = color.split("(\\s*,\\s*|\\s+)");
            if (color_values.length >= 3 ) {
                fill_r = Short.parseShort(color_values[0]);
                fill_g = Short.parseShort(color_values[1]);
                fill_b = Short.parseShort(color_values[2]);
            }
            if (color_values.length >= 4) {
                fill_a = Short.parseShort(color_values[3]);
            }
            return Mask.toRGB(fill_r,fill_g,fill_b,fill_a);
        }

        public boolean shouldBlurMask() {
            return mask_blur > 0;
        }

        public int getMaskBlurSize() {
            return mask_blur;
        }

        public boolean hasTexture() {
            return texture != null && texture.trim().length() != 0;
        }

        public BufferedImage getTexture() {
            if (textureImage == null) {
                try {
                    Path textureFolder = getTextureFolder();
                    if (texture != null && textureFolder != null) {
                        Path texture = textureFolder.resolve(this.texture);
                        textureImage = ImageIO.read(texture.toFile());
                    }
                }
                catch (IOException ignored) {
                    CommandLineUtils.printError("Could not load texture image for texture " + getName());
                    CommandLineUtils.reportException(ignored);
                }
            }
            return textureImage;
        }

        public NodedFunction getZMaskFilter() {
            if (z_mask_bezier_definitions == null || z_mask_bezier_definitions.length() == 0) return null;

            return NodedFunction.fromBezierCoordinateString(z_mask_bezier_definitions,200,0,1,127.5,-128.5);
        }

        public NodedFunction getAngleFilter() {
            if (angle_mask_bezier_definitions == null || angle_mask_bezier_definitions.length() == 0) return null;
            return NodedFunction.fromBezierCoordinateString(angle_mask_bezier_definitions,200,0,1,127.5,-128.5);
        }

        public Mask.BlendMode getBlendMode() {
            if (blend_mode == null) return Mask.BlendMode.NONE;
            String bmode = blend_mode.toLowerCase();
            if (bmode.contains("screen")) return Mask.BlendMode.SCREEN;
            if (bmode.contains("multiply")) return Mask.BlendMode.MULTIPLY;
            if (bmode.contains("overlay")) return Mask.BlendMode.OVERLAY;
            return Mask.BlendMode.NONE;
        }

        public int getDilateSize() {
            return dilate_size;
        }

        public int getErodeSize() {
            return erode_size;
        }

        public int getBlurSize() {
            return blur_size;
        }

        public boolean shouldErodeFirst() {
            return erode_first;
        }
    }

    // todo(MS): move to PropertiesLoader???
    @Nullable
    public static Path getTextureFolder() {

        PropertiesLoader.update();

        String specifiedTextureFolder = PropertiesLoader.texture.texture_folder;
        if (specifiedTextureFolder == null) {
            CommandLineUtils.printWarning("Property texture_folder is not set in properties.ini file");
            return null;
        }

        Path textureFolderPath = Paths.get(specifiedTextureFolder);
        if (textureFolderPath.isAbsolute()) {
            if (!Files.exists(textureFolderPath) || !Files.isDirectory(textureFolderPath)) {
                CommandLineUtils.printWarning("Property texture_folder in properties.ini file does nt point to existing directory: " + textureFolderPath);
                return null;
            }
            return textureFolderPath;
        }

        // path is relative
        Path[] baseFolders = new Path[] {
                Paths.get(".").toAbsolutePath(),
                Paths.get(OutputUtils.GetExecutionPath()).toAbsolutePath()
        };

        for (Path baseFolder : baseFolders) {
            Path path = baseFolder.resolve(textureFolderPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                return path.toAbsolutePath();
            }
        }
        CommandLineUtils.printWarning("Property texture_folder in properties.ini file does nt point to existing directory: " + textureFolderPath);
        return null;
    }

    private ArrayList< ArrayList<OcadVertex> > splitByHoleFirst(ArrayList<OcadVertex> vertices) {
        ArrayList< ArrayList<OcadVertex> > ret = new ArrayList<>();

        ArrayList<OcadVertex> buf = new ArrayList<>();

        for (OcadVertex v : vertices) {
            if (v.HOLE_FIRST) { // Hole first encountered, enclose linestring 'buf' and put it into ret.
                if (buf.size() >= 3) {
                    buf.add(buf.get(0));
                    ret.add(buf);
                }
                buf = new ArrayList<>();
            }
            buf.add(v);
        }
        // Enclose buf residue and add it into ret.
        if (buf.size() >= 3) {
            buf.add(buf.get(0));
            ret.add(buf);
        }
        return ret;
    }

    public Geometry objToGeometry(TOcadObject obj, GeometryFactory gf) throws Exception {
        if (obj.Otp == 2) { // Line Object
            CurveString cs = CurveString.fromOcadVertices(obj.vertices);
            return cs.interpolate(gf);
        }
        else if (obj.Otp == 3) { // Area object
            // Run through vertices and add them to buffer, while hole first not encountered...

            // Exterior ring - first element of this array, other are holes
            ArrayList< ArrayList<OcadVertex> > exteriorAndHoles = splitByHoleFirst(obj.vertices);
            if (exteriorAndHoles.size() == 0) {
                throw new Exception("Ocad object has no geometry");
            }

            ArrayList<OcadVertex> exterior = exteriorAndHoles.get(0);
            ArrayList< ArrayList<OcadVertex> > holes = new ArrayList<>();
            for (int i = 1; i < exteriorAndHoles.size(); ++i) {
                holes.add(exteriorAndHoles.get(i));
            }

            LineString exteriorString = CurveString.fromOcadVertices(exterior).interpolate(gf);
            LinearRing exteriorRing = gf.createLinearRing(exteriorString.getCoordinateSequence());
            ArrayList<LinearRing> holeRings = new ArrayList<>();
            holes.forEach(hole -> {
                try {
                    LineString holeString = CurveString.fromOcadVertices(hole).interpolate(gf);
                    holeRings.add(gf.createLinearRing(holeString.getCoordinateSequence()));
                }
                catch (Exception e) {
                    System.out.println("Invalid area object: " + e.getMessage());
                }
            });

            // Rings gathered. Now transform them into polygon
            return gf.createPolygon(exteriorRing, holeRings.toArray(new LinearRing[holeRings.size()]));
        }
        else if (obj.Otp == 7) { // Rectangle Object
            // border
            if (DeserializedOCAD.matchesMask(obj.Sym, PropertiesLoader.ocad_input.border)) {
                Geometry coordinates = getLineRingForBorder(obj, gf);
                if (coordinates != null) {
                    return coordinates;
                }
            }
            else {
                // todo(MS): support other rectangle objects
            }
        }
        else if (obj.Otp == 1) { // Point Object
            // slope marks
            if (DeserializedOCAD.matchesMask(obj.Sym, PropertiesLoader.ocad_input.slope)) {
                return new SlopeMark(obj).asGeometry(Constants.slope_length, gf);
            }
            else {
                // todo(MS): support other point objects
            }
        }
        return null;
    }

    @Nullable
    public static LinearRing getLineRingForBorder(TOcadObject obj, GeometryFactory gf) {
        ArrayList<OcadVertex> vertices = obj.vertices;
        if (!vertices.isEmpty()) {
            OcadVertex firstVertex = vertices.get(0);
            OcadVertex lastVertex = vertices.get(vertices.size() - 1);
            if (!firstVertex.equals(lastVertex)) {
                vertices.add(firstVertex);
            }
            Coordinate[] coordinates = vertices.toArray(new OcadVertex[vertices.size()]);
            return gf.createLinearRing(coordinates);
        }
        return null;
    }

    public void splitAndWriteToFile(BufferedImage image, String outputPath) {
        if (deserializedOCAD.border == null) {
            return;
        }
        GeometryFactory geometryFactory = new GeometryFactory();
        LinearRing borderLineRing = PatchTextureGenerator.getLineRingForBorder(deserializedOCAD.border, geometryFactory);
        if (borderLineRing == null) {
            return;
        }

        PointRasterizer rasterizer = patch.getTextureRasterizer();
        CommandLineUtils.reportProgressBegin("Splitting textures");

        PointRasterizer maskRasterizer = patch.getMaskRasterizer();
        Mask layer = new Mask(maskRasterizer.getColumnCount(), maskRasterizer.getRowCount());


        BufferedImage textureImage = new BufferedImage(rasterizer.getColumnCount(), rasterizer.getRowCount(), BufferedImage.TYPE_INT_ARGB);

    }

    public void generateAndWriteToFile(String path) {
        GeometryFactory geometryFactory = new GeometryFactory();
        PointRasterizer rasterizer = patch.getTextureRasterizer();

        CommandLineUtils.reportProgressBegin("Rasterizing textures");

        PointRasterizer maskRasterizer = patch.getMaskRasterizer();
        Mask layer = new Mask(maskRasterizer.getColumnCount(), maskRasterizer.getRowCount());

        BufferedImage textureImage = new BufferedImage(rasterizer.getColumnCount(), rasterizer.getRowCount(), BufferedImage.TYPE_INT_ARGB);
        int brushNumber = -1;
        for (Brush brush : brushes) {
            brushNumber++;
            CommandLineUtils.reportProgress(brushNumber, brushes.size());

            if (brush.isOverlay() && brush.shouldFill()) {
                layer.fill((byte)127);
            }
            else {
                if (deserializedOCAD == null) {
                    continue;
                }
                layer.clean();

                List<Polygon> polygons = new ArrayList<>();
                List<LineString> strings = new ArrayList<>();

                List<TOcadObject> objects = deserializedOCAD.getObjectsByMask(brush.symbol_ids);
                for (TOcadObject obj : objects) {
                    Geometry geometry = geometryCache.get(obj);
                    if (geometry == null) {
                        try {
                            geometry = objToGeometry(obj, geometryFactory);
                        }
                        catch (Exception ex) {
                            System.out.println("\nFailed to read geometry for " + obj.Sym + " " + obj.getType());
                        }
                    }
                    if (geometry == null) {
                        continue;
                    }

                    if (Polygon.class.isAssignableFrom(geometry.getClass()) && brush.shouldFill()) {
                        Polygon poly = (Polygon) geometry;
                        polygons.add(poly);
                    }
                    else if (LineString.class.isAssignableFrom(geometry.getClass())) {
                        LineString ls = (LineString) geometry;
                        strings.add(ls);
                    }
                }

                layer.fillPolygons(polygons, maskRasterizer, (byte) (127));
                layer.drawLines(strings, maskRasterizer, (byte)(127), brush.width);
            }

            // Apply height mask
            NodedFunction nf = brush.getZMaskFilter();
            Coordinate ret = new Coordinate();
            if (nf != null) {
                for (int row = 0; row != layer.height; ++row) {
                    for (int column = 0; column != layer.width; ++column) {

                        double u = GeomUtils.map(column+0.5,0,layer.width,0,1);
                        double v = GeomUtils.map(row+0.5,0,layer.height,0,1);

                        patch.UVtoXY(u,v,ret);
                        double x = ret.x;
                        double y = ret.y;
                        double z = mesh.getPointHeight(x,y);
                        if (Double.isNaN(z)) {
                            layer.pixels[row*layer.width+column] = -128;
                        } else {
                            layer.pixels[row*layer.width+column] =
                                    (byte)GeomUtils.clamp( nf.apply( GeomUtils.map(z,mesh.z_min,mesh.z_max,0,1) ), -128, 127);
                        }
                    }
                }
            }

            // Calculate angle
            nf = brush.getAngleFilter();
            if (nf != null) {
                for (int row = 0; row != layer.height; ++row) {
                    for (int column = 0; column != layer.width; ++column) {

                        double u = GeomUtils.map(column + 0.5, 0, layer.width, 0, 1);
                        double v = GeomUtils.map(row + 0.5, 0, layer.height, 0, 1);

                        patch.UVtoXY(u,v,ret);
                        double x = ret.x;
                        double y = ret.y;
                        double angle = mesh.getPointAngle(x,y);
                        if (Double.isNaN(angle)) {
                            layer.pixels[row*layer.width+column] = -128;
                        } else {
                            layer.pixels[row*layer.width+column] =
                                    (byte)GeomUtils.clamp( nf.apply( angle ), -128, 127);
                        }
                    }
                }
            }
            if (brush.getBlurSize() > 0) {
                RasterUtils.gauss(layer.pixels,layer.width,layer.height,brush.getBlurSize(),3);
            }

            Coordinate texture_min = patch.XYtoUV(0,0);
            double textureScale = PropertiesLoader.texture.scale;
            Coordinate texture_max = patch.XYtoUV(textureImage.getWidth() * textureScale, textureImage.getHeight() * textureScale);

            texture_min.x = GeomUtils.map(texture_min.x, 0, 1, 0, textureImage.getWidth());
            texture_min.y = GeomUtils.map(texture_min.y, 0, 1, 0, textureImage.getHeight());
            texture_max.x = GeomUtils.map(texture_max.x, 0, 1, 0, textureImage.getWidth());
            texture_max.y = GeomUtils.map(texture_max.y, 0, 1, 0, textureImage.getHeight());

            Envelope textureEnvelope = new Envelope(texture_min, texture_max);

            layer.calcImagePixels(textureImage.getWidth(), textureImage.getHeight());

            // Write layer to texture
            if (brush.hasTexture()) {
                BufferedImage texture = brush.getTexture();
                if (texture == null) {
                    CommandLineUtils.printWarning("Could not load texture for brush: " + brush.getName() + ", skipping");
                } else {
                    layer.overlay(textureImage, texture, brush.getBlendMode(), textureEnvelope);
                }
            } else {
                layer.overlay(textureImage, brush.getColor(), brush.getBlendMode());
            }

        }
        CommandLineUtils.reportProgressEnd();
        RasterUtils.save(textureImage,path);
        CommandLineUtils.reportFinish();
    }

    public static List<Brush> loadBrushes(Path textureFolder) {
        List<Brush> brushes = new ArrayList<>();
        try {
            Files.walk(textureFolder).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (OutputUtils.getExtension(filePath.toString()).equals("vmt")) {
                        try {
                            FileReader reader = new FileReader(filePath.toString());
                            Brush brush = new Gson().fromJson(reader, Brush.class);
                            brushes.add(brush);
                        } catch (Exception ignored) {
                            CommandLineUtils.printWarning("Failed reading " + filePath.toString() + " reason: " + ignored.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            CommandLineUtils.printWarning("Could not find textures folder: " + e.getMessage());
            return null;
        }
        // Sort brushes by z-index
        brushes.sort((lhs, rhs)->Integer.compare(lhs.z_index, rhs.z_index));
        return brushes;
    }
}
