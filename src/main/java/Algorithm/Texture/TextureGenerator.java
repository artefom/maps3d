package Algorithm.Texture;

import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import Deserialization.Interpolation.CurveString;
import Utils.*;
import com.vividsolutions.jts.geom.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.google.gson.*;

import static Utils.Constants.DESERIALIZATION_BEZIER_STEP;

/**
 * generates texture for map
 */
public class TextureGenerator {

    DeserializedOCAD map;
    public TextureGenerator(DeserializedOCAD map) {
        this.map = map;
    }

    private String GetExecutionPath(){
        String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
        absolutePath = absolutePath.replaceAll("%20"," "); // Surely need to do this here
        String osAppropriatePath = System.getProperty( "os.name" ).contains( "indow" ) ? absolutePath.substring(1) : absolutePath;
        return osAppropriatePath;
    }



    private class Brush {
        public String name = "Unknown";
        public String symbol_ids = "";
        public boolean filled;
        public String color;
        public int z_index = -1;
        public int mask_blur = 0;

        public boolean shouldFill() {
            return filled;
        }

        public boolean isBackground() {
            for (Integer i : getSymbols()) {
                if (i == -1) return true;
            }
            return false;
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
            return Texture.toRGB(fill_r,fill_g,fill_b,fill_a);
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
    }

    public void writeToFile(String path, PointRasterizer rast) {


        GeometryFactory gf = new GeometryFactory();

        int count = 0;
        int skip_count = 0;
        int error_count = 0;

        String textures_folder = GetExecutionPath()+"/textures";
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
            System.out.println("Could not find ./textures folder");
            return;
        };

        Texture tex = new Texture(rast.getColumnCount(),rast.getRowCount());

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
        for (int i = 0; i != brushes.size(); ++i) {
            CommandLineUtils.reportProgress(i+1,brushes.size());
            Brush b = brushes.get(i);

            if (b.isBackground() && b.shouldFill()) {
                tex.fill(b.getColor());
                continue;
            }

            int fill_color = b.getColor();

            //BufferedImage mask_buf = new BufferedImage(rast.getColumnCount(),rast.getRowCount(),BufferedImage.TYPE_USHORT_GRAY);

            List<Polygon> polygons = new ArrayList<>();
            List<LineString> strings = new ArrayList<>();
            for (TOcadObject obj : map.getObjectsByIDs(b.getSymbols())) {
                count += 1;
                Geometry col;
                try {
                    col = obj.getGeometry(gf);
                } catch (Exception ex) {
                    error_count += 1;
                    throw new RuntimeException(ex.getMessage());
                }
                if (col == null) {
                    skip_count += 1;
                    continue;
                }

                if ( Polygon.class.isAssignableFrom(col.getClass()) && b.shouldFill() ) {
                    Polygon poly = (Polygon)col;

                    count += 1;
                    polygons.add(poly);
                    strings.add(poly.getExteriorRing());
                    for (int ringN = 0; ringN != poly.getNumInteriorRing(); ++ringN) {
                        strings.add(poly.getInteriorRingN(ringN));
                    }
                } else if ( LineString.class.isAssignableFrom(col.getClass()) ) {
                    LineString ls = (LineString)col;
                    count+=1;
                    strings.add(ls);
                }
            }

            tex.fillPolygons(polygons,rast,b.getColor());
            tex.drawLines(strings,rast,b.getColor(),4);
//
//                float[][] mask = rast.createFloatBuffer();
//                rast.fillPolygons(mask, polygons, 1);
//
//
//                if (b.shouldBlurMask()) {
//                    RasterUtils.gauss(mask,b.getMaskBlurSize(),2);
//                }
//
//                RasterUtils.saveAsPng(mask, path + "_" + b.name + "_mask");
//                tex.fill(mask, fill_color);

        }
        CommandLineUtils.reportProgressEnd();


        tex.save(path+".png");

        CommandLineUtils.report();
    }
}
