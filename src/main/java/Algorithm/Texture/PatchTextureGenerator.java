package Algorithm.Texture;

import Algorithm.Mesh.Mesh3D;
import Algorithm.Mesh.TexturedPatch;
import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import Utils.*;
import Utils.Properties.PropertiesLoader;
import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * generates texture for map
 */
public class PatchTextureGenerator {

    DeserializedOCAD map;
    TexturedPatch patch;
    Mesh3D mesh;

    public PatchTextureGenerator(DeserializedOCAD map, TexturedPatch texturedPatch, Mesh3D mesh) {
        this.map = map;
        this.patch = texturedPatch;
        this.mesh = mesh;
    }

    private class Brush {
        public String name = "Unknown";
        public String symbol_ids = "";
        public boolean filled;
        public String color;
        public int z_index = -1;
        public int mask_blur = 0;
        public String z_mask_bezier_definitions = "";
        public String angle_mask_bezier_definitions = "";
        public String texture;

        public boolean erode_first;
        public int dilate_size;
        public int erode_size;
        public int blur_size;

        public String blend_mode = "";

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
            String[] color_values = color.split("(\\s*,\\s*|\\s)");
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

        public ArrayList<Integer> getSymbols() {
            String[] symbols = symbol_ids.split("(\\s*,\\s*|\\s)");
            ArrayList<Integer> ret = new ArrayList<>();
            for (int i = 0; i != symbols.length; ++i) {
                try {
                     ret.add(Integer.parseInt(symbols[i]));
                } catch (Exception ex) {
                    CommandLineUtils.printWarning("Error parsing symbols at "+name+" material");
                }
            }
            return ret;
        }

        public boolean shouldBlurMask() {
            return mask_blur > 0;
        }

        public int getMaskBlurSize() {
            return mask_blur;
        }

        public boolean hasTexture() {
            return texture != null && texture.length() != 0;
        }

        public BufferedImage getTexture() {
            BufferedImage img = null;
            try {
                String texture_folder = getTextureFolder();
                String filename = texture_folder+"/"+this.texture;
                img = ImageIO.read(new File(filename));
            } catch (IOException ignored) {
                CommandLineUtils.printError("Could not load texture image for texture "+getName());
                CommandLineUtils.reportException(ignored);
            }
            return img;
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

    public static String getTextureFolder() {

        PropertiesLoader.update();

        String[] folders = new String[] {
                (new File("")).getAbsolutePath(),
                OutputUtils.GetExecutionPath(),
                "" };

        String specified_texture_folder = PropertiesLoader.texture.texture_folder;

        if (specified_texture_folder.charAt(0) == '.') specified_texture_folder = specified_texture_folder.substring(1);
        if (specified_texture_folder.charAt(0) != '\\' &&
                specified_texture_folder.charAt(0) != '/') specified_texture_folder = '\\'+specified_texture_folder;

        for (String folder : folders) {
            while (folder.length() > 0 && folder.charAt(folder.length()-1) == '\\' || folder.charAt(folder.length()-1) == '/')
                folder = folder.substring(0,folder.length()-1);

            Path path = Paths.get( folder+specified_texture_folder );
            File f = path.toFile();

            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        return null;
    }

    public void writeToFile(String path) {


        GeometryFactory gf = new GeometryFactory();

        PointRasterizer rast = patch.getTextureRasterizer();

        //float[] sobel = RasterUtils.sobel(heightmap,rast.getColumnCount(),rast.getRowCount());

        String textures_folder = getTextureFolder(); //OutputUtils.GetExecutionPath()+"/textures";

        ArrayList<Path> vmtFiles = new ArrayList<>();
        try {
            Files.walk(Paths.get(textures_folder)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (OutputUtils.getExtension(filePath.toString()).equals("vmt")) {
                        vmtFiles.add(filePath);
                    }
                }
            });
        } catch (Exception ignored) {
            CommandLineUtils.printWarning("Could not find textures folder");
            return;
        };

        BufferedImage tex = new BufferedImage(rast.getColumnCount(),rast.getRowCount(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D tex_g = (Graphics2D)tex.getGraphics();

        List<Brush> brushes = new ArrayList<>();
        for (Path p : vmtFiles) {
            try {
                FileReader reader = new FileReader(p.toString());
                Gson g = new Gson();
                Brush b = g.fromJson(reader, Brush.class);
                brushes.add(b);
            } catch (Exception ignored) {
                CommandLineUtils.printWarning("Failed reading " + p.toString() + " reason: " + ignored.getMessage());
            }
        }

        // Sort by z-index
        brushes.sort((lhs,rhs)->Integer.compare(lhs.z_index,rhs.z_index));

        CommandLineUtils.reportProgressBegin("Rasterizing textures");

        int current = 1;
        int total = brushes.size();

        PointRasterizer maskRast = patch.getMaskRasterizer();
        Mask layer = new Mask(maskRast.getColumnCount(),maskRast.getRowCount());

        for (int i = 0; i != brushes.size(); ++i) {


            CommandLineUtils.reportProgress(i,brushes.size());
            Brush b = brushes.get(i);

            if (b.isOverlay() && b.shouldFill()) {
                layer.fill((byte)127);
//                tex_g.setBackground( new Color(b.getColor()) );
//                tex_g.fillRect(0,0,rast.getColumnCount(),rast.getRowCount());
            } else {

                if (map == null) continue;

                layer.clean();
                //BufferedImage mask_buf = new BufferedImage(rast.getColumnCount(),rast.getRowCount(),BufferedImage.TYPE_USHORT_GRAY);

                List<Polygon> polygons = new ArrayList<>();
                List<LineString> strings = new ArrayList<>();

                map.getObjectsByIDs(b.getSymbols());
                for (TOcadObject obj : map.getObjectsByIDs(b.getSymbols())) {
                    Geometry col = null;
                    try {
                        col = obj.getGeometry(gf);
                    } catch (Exception ex) {
                        //System.out.println("Failed to read geometry for " + obj.Sym + " " + obj.getType());
                    }
                    if (col == null) {
                        continue;
                    }

                    if (Polygon.class.isAssignableFrom(col.getClass()) && b.shouldFill()) {
                        Polygon poly = (Polygon) col;

                        polygons.add(poly);
//                        strings.add(poly.getExteriorRing());
//                        for (int ringN = 0; ringN != poly.getNumInteriorRing(); ++ringN) {
//                            strings.add(poly.getInteriorRingN(ringN));
//                        }
                    } else if (LineString.class.isAssignableFrom(col.getClass())) {
                        LineString ls = (LineString) col;
                        strings.add(ls);
                    }
                }

                layer.fillPolygons(polygons, maskRast, (byte) (127));
                layer.drawLines(strings, maskRast, (byte)(127), 4);
            }

            // Apply height mask
            NodedFunction nf = b.getZMaskFilter();
            Coordinate ret = new Coordinate();
            if (nf != null) {
//                for (int j = 0; j != layer.pixels.length; ++j) {
//                    layer.pixels[j] = (byte)GeomUtils.map(j,0,layer.pixels.length,-128,127);
//                }
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

            //Calculate angle
            nf = b.getAngleFilter();
            if (nf != null) {
//                for (int j = 0; j != layer.pixels.length; ++j) {
//                    layer.pixels[j] = (byte)GeomUtils.map(j,0,layer.pixels.length,-128,127);
//                }
                for (int row = 0; row != layer.height; ++row) {
                    for (int column = 0; column != layer.width; ++column) {

                        double u = GeomUtils.map(column+0.5,0,layer.width,0,1);
                        double v = GeomUtils.map(row+0.5,0,layer.height,0,1);

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
//            if (nf != null) {
//                for (int pixel = 0; pixel != layer.pixels.length; ++pixel) {
//                    layer.pixels[pixel] = (byte)(nf.apply(Math.atan(sobel[pixel]*50)/1.5)); //(byte)(GeomUtils.clamp( 255.0*heightmap[pixel], 0, 255 ) - 128 );
//                }
//            }

//            if (b.shouldErodeFirst()) {
//                if (b.getErodeSize() != 0) RasterUtils.erode(layer.pixels,layer.width,layer.height,b.getErodeSize());
//                if (b.getDilateSize() != 0) RasterUtils.dilate(layer.pixels,layer.width,layer.height,b.getDilateSize());
//            } else {
//                if (b.getDilateSize() != 0) RasterUtils.dilate(layer.pixels,layer.width,layer.height,b.getDilateSize());
//                if (b.getErodeSize() != 0) RasterUtils.erode(layer.pixels,layer.width,layer.height,b.getErodeSize());
//            }

            if (b.getBlurSize() > 0) {
                RasterUtils.gauss(layer.pixels,layer.width,layer.height,b.getBlurSize(),3);
            }

//            Envelope textureEnvelope = new Envelope(
//                    patch.XYtoUV(0,0),
//                    patch.XYtoUV(tex.getWidth()/patch.getTexturePointsPerUnit(),tex.getHeight()/patch.getTexturePointsPerUnit())
//            );


            Coordinate texture_min = patch.XYtoUV(0,0);
            Coordinate texture_max = patch.XYtoUV(tex.getWidth()*PropertiesLoader.texture.scale,tex.getHeight()*PropertiesLoader.texture.scale);

            texture_min.x = GeomUtils.map(texture_min.x,0,1,0,tex.getWidth());
            texture_min.y = GeomUtils.map(texture_min.y,0,1,0,tex.getHeight());
            texture_max.x = GeomUtils.map(texture_max.x,0,1,0,tex.getWidth());
            texture_max.y = GeomUtils.map(texture_max.y,0,1,0,tex.getHeight());

            Envelope textureEnvelope = new Envelope(texture_min,texture_max);

            layer.calcImgPixels(tex.getWidth(),tex.getHeight());

            //Write layer to texture
            if (b.hasTexture()) {
                BufferedImage texture = b.getTexture();
                if (texture == null) {
                    CommandLineUtils.printWarning("Could not load texture for brush: "+b.getName()+", skipping");
                } else {
                    layer.overlay(tex, texture, b.getBlendMode(), textureEnvelope);
                }
            } else {
                layer.overlay(tex, b.getColor(), b.getBlendMode());
            }

        }
        CommandLineUtils.reportProgressEnd();

        RasterUtils.save(tex,path);

        CommandLineUtils.report();
    }
}
