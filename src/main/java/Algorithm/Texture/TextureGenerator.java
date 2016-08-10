package Algorithm.Texture;

import Deserialization.Binary.TOcadObject;
import Deserialization.DeserializedOCAD;
import Deserialization.Interpolation.CurveString;
import Utils.Constants;
import Utils.LineStringIterator;
import Utils.PointRasterizer;
import Utils.RasterUtils;
import com.vividsolutions.jts.geom.*;

import java.util.ArrayList;

import static Utils.Constants.DESERIALIZATION_BEZIER_STEP;

/**
 * generates texture for map
 */
public class TextureGenerator {

    DeserializedOCAD map;
    public TextureGenerator(DeserializedOCAD map) {
        this.map = map;
    }

    public void writeToFile(String path, PointRasterizer rast) {

        double[][] result = rast.createDoubleBuffer(0);

        GeometryFactory gf = new GeometryFactory();

        int count = 0;
        int skip_count = 0;
        int error_count = 0;
        for (Integer print_sym_id : map.symbol_ids) {

            for (TOcadObject obj : map.getObjectsByID(print_sym_id)) {

                //if (obj.Sym != 301000) continue;
                Geometry col;
                try {
                    col = obj.getGeometry(gf);
                } catch (Exception ex) {
                    throw new RuntimeException(ex.getMessage());
                }
                if (col == null) continue;
                for (int i = 0; i != col.getNumGeometries(); ++i) {

                    LineString ls = (LineString)col.getGeometryN(i);

                    count += 1;
                    LineSegment buf = new LineSegment();
                    LineStringIterator it = new LineStringIterator(ls, buf);
                    while (it.hasNext()) {
                        it.next();
                        rast.rasterize(result, buf, 1);
                    }
                }
            }

        }

        System.out.println("Total: " + count);
        System.out.println("Skipped: " + skip_count);
        System.out.println("Failed: "+error_count);

        RasterUtils.saveAsPng(result,path);
    }
}
