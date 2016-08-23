package Algorithm.Texture;

import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import Utils.*;
import com.vividsolutions.jts.geom.*;

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
import java.util.StringTokenizer;

import com.google.gson.*;
import com.vividsolutions.jts.geom.Polygon;

import javax.imageio.ImageIO;

/**
 * generates texture for map
 */
public class TextureGenerator {

    DeserializedOCAD map;
    public TextureGenerator(DeserializedOCAD map) {
        this.map = map;
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
                String filename = OutputUtils.GetExecutionPath()+"/textures/"+this.texture;
                img = ImageIO.read(new File(filename));
            } catch (IOException ignored) {
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

    public void writeToFile(String path, PointRasterizer rast, float[] heightmap) {


        GeometryFactory gf = new GeometryFactory();


        float[] sobel = RasterUtils.sobel(heightmap,rast.getColumnCount(),rast.getRowCount());

        String textures_folder = OutputUtils.GetExecutionPath()+"/textures";
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
            CommandLineUtils.printWarning("Could not find ./textures folder");
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

        Mask layer = new Mask(rast.getColumnCount(),rast.getRowCount());
        for (int i = 0; i != brushes.size(); ++i) {


            CommandLineUtils.reportProgress(i,brushes.size());
            Brush b = brushes.get(i);

            if (b.isOverlay() && b.shouldFill()) {
                layer.fill((byte)127);
//                tex_g.setBackground( new Color(b.getColor()) );
//                tex_g.fillRect(0,0,rast.getColumnCount(),rast.getRowCount());
            } else {
                layer.clean();
                //BufferedImage mask_buf = new BufferedImage(rast.getColumnCount(),rast.getRowCount(),BufferedImage.TYPE_USHORT_GRAY);

                List<Polygon> polygons = new ArrayList<>();
                List<LineString> strings = new ArrayList<>();
                for (TOcadObject obj : map.getObjectsByIDs(b.getSymbols())) {
                    Geometry col;
                    try {
                        col = obj.getGeometry(gf);
                    } catch (Exception ex) {
                        throw new RuntimeException("Check texture .vmt files, they are probably invalid");
                    }
                    if (col == null) {
                        continue;
                    }

                    if (Polygon.class.isAssignableFrom(col.getClass()) && b.shouldFill()) {
                        Polygon poly = (Polygon) col;

                        polygons.add(poly);
                        strings.add(poly.getExteriorRing());
                        for (int ringN = 0; ringN != poly.getNumInteriorRing(); ++ringN) {
                            strings.add(poly.getInteriorRingN(ringN));
                        }
                    } else if (LineString.class.isAssignableFrom(col.getClass())) {
                        LineString ls = (LineString) col;
                        strings.add(ls);
                    }
                }

                layer.fillPolygons(polygons, rast, (byte) (127));
                layer.drawLines(strings, rast, (byte)(127), 4);
            }

            //RasterUtils.dilate_size(layer.pixels,layer.width,layer.height);
            //RasterUtils.erode(layer.pixels,layer.width,layer.height);
            // Apply height mask
            NodedFunction nf = b.getZMaskFilter();
            if (nf != null) {
                for (int pixel = 0; pixel != layer.pixels.length; ++pixel) {
                    layer.pixels[pixel] = (byte)(nf.apply(heightmap[pixel]).byteValue()); //(byte)(GeomUtils.clamp( 255.0*heightmap[pixel], 0, 255 ) - 128 );
                }
            }

            nf = b.getAngleFilter();
            if (nf != null) {
                for (int pixel = 0; pixel != layer.pixels.length; ++pixel) {
                    layer.pixels[pixel] = (byte)(nf.apply(Math.atan(sobel[pixel]*50)/1.5).byteValue()); //(byte)(GeomUtils.clamp( 255.0*heightmap[pixel], 0, 255 ) - 128 );
                }
            }

            if (b.shouldErodeFirst()) {
                for (int step = 0; step != b.getErodeSize(); ++step) {
                    RasterUtils.erode(layer.pixels,layer.width,layer.height);
                }
                for (int step = 0; step != b.getDilateSize(); ++step) {
                    RasterUtils.dilate(layer.pixels,layer.width,layer.height);
                }
            } else {
                for (int step = 0; step != b.getDilateSize(); ++step) {
                    RasterUtils.dilate(layer.pixels,layer.width,layer.height);
                }
                for (int step = 0; step != b.getErodeSize(); ++step) {
                    RasterUtils.erode(layer.pixels,layer.width,layer.height);
                }
            }

            if (b.getBlurSize() != 0) {
                RasterUtils.gauss(layer.pixels,layer.width,layer.height,b.getBlurSize(),3);
            }

            //Write layer to texture
            if (b.hasTexture()) {
                layer.overlay(tex, b.getTexture(), b.getBlendMode());
            } else {
                layer.overlay(tex, b.getColor(), b.getBlendMode());
            }

        }
        CommandLineUtils.reportProgressEnd();

        RasterUtils.save(tex,path+".png");

        CommandLineUtils.report();
    }
}
