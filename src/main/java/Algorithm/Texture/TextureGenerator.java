package Algorithm.Texture;

import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import Deserialization.Interpolation.CurveString;
import Utils.Constants;
import Utils.LineStringIterator;
import Utils.PointRasterizer;
import Utils.RasterUtils;
import com.vividsolutions.jts.geom.*;

import java.io.File;
import java.io.FileReader;
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


    private class Fill {
        public String texture;
        public String color;
    }
    private class Brush {
        public String name;
        public int symbol_id;
        public Fill fill;
    }


    public void writeToFile(String path, PointRasterizer rast) {

        double[][] result = rast.createDoubleBuffer(0);

        GeometryFactory gf = new GeometryFactory();

        int count = 0;
        int skip_count = 0;
        int error_count = 0;

        String textures_folder = GetExecutionPath()+"/textures";
        ArrayList<Path> vmtFiles = new ArrayList<>();
        try {
            Files.walk(Paths.get(textures_folder)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    String extension = "";
                    String fpath = filePath.toString();
                    int i = fpath.lastIndexOf('.');
                    int p = Math.max(fpath.lastIndexOf('/'), fpath.lastIndexOf('\\'));

                    if (i > p) {
                        extension = fpath.substring(i+1);
                    }
                    if (extension.equals("vmt")) {
                        vmtFiles.add(filePath);
                    }
                }
            });
        } catch (Exception ignored) {
            System.out.println("Could not find ./textures folder");
            return;
        };

        System.out.println("Found vmt files:");
        for (Path p : vmtFiles) {
            System.out.println(p.toString());

            try {
                FileReader reader = new FileReader(p.toString());

                Gson g = new Gson();
                Brush b = g.fromJson(reader, Brush.class);

                List<Polygon> polygons = new ArrayList<>();
                for (TOcadObject obj : map.getObjectsByID(b.symbol_id)) {
                    count+=1;
                    Geometry col;
                    try {
                        col = obj.getGeometry(gf);
                    } catch (Exception ex) {
                        error_count+=1;
                        throw new RuntimeException(ex.getMessage());
                    }
                    if (col == null) {skip_count+=1;continue;};
                    count+=1;
                    polygons.add((Polygon)col);
                }
                rast.fillPolygons(result,polygons,1);

            } catch (Exception ignored) {

            }
        }

//        List<Polygon> polygons = new ArrayList<>();
//        for (Integer print_sym_id : map.symbol_ids) {
//
//            for (TOcadObject obj : map.getObjectsByID(print_sym_id)) {
//
////                if (obj.Sym != 301000) {
////                    skip_count+=1;
////                    continue;
////                }
//                Geometry col;
//                try {
//                    col = obj.getGeometry(gf);
//                } catch (Exception ex) {
//                    error_count+=1;
//                    throw new RuntimeException(ex.getMessage());
//                }
//                if (col == null) {skip_count+=1;continue;};
//                count+=1;
//                polygons.add((Polygon)col);
//                //rast.fillPolygons(result,col,1);
//            }
//
//        }
        //rast.fillPolygons(result,polygons,1);

        System.out.println("Total: " + count);
        System.out.println("Skipped: " + skip_count);
        System.out.println("Failed: "+error_count);

        RasterUtils.saveAsPng(result,path);
    }
}
