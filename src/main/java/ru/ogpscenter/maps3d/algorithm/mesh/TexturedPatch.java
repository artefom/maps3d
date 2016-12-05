package ru.ogpscenter.maps3d.algorithm.mesh;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import ru.ogpscenter.maps3d.utils.*;
import ru.ogpscenter.maps3d.utils.properties.PropertiesLoader;

import java.util.*;

/**
 * Created by Artyom.Fomenko on 31.08.2016.
 */
public class TexturedPatch {

    private ArrayList<Coordinate> textureCoordinates;
    private Coordinate[] vertexes;

    public Collection<int[]> getTriangles() {
        return triangles;
    }

    private Collection<int[]> triangles;

    private HashMap<Integer,Integer> vertexesToTextureCoordinates;

    private Envelope coordinatesEnvelope;
    private Envelope uvEnvelope;

    private int vertexCount;
    private int triangleCount;

    private double edgePadding;
    private boolean mantainAspect;

    public double getTexturePointsPerUnit() {
        return texturePointsPerUnit;
    }

    public void setTexturePointsPerUnit(double texturePointsPerUnit) {
        this.texturePointsPerUnit = texturePointsPerUnit;
    }

    public double getTextureXShift() {
        return textureXShift;
    }

    public void setTextureXShift(double textureXShift) {
        this.textureXShift = textureXShift;
    }

    public double getTextureYShift() {
        return textureYShift;
    }

    public void setTextureYShift(double textureYShift) {
        this.textureYShift = textureYShift;
    }

    public int getTexturePatchWidth() {
        return PropertiesLoader.textured_patch.texture_width;
    }

    public int getTexturePatchHeight() {
        return PropertiesLoader.textured_patch.texture_height;
    }

    public double getMaskPointsPerUnit() {
        return PropertiesLoader.textured_patch.mask_points_per_unit;
    }

    public void setMaskPointsPerUnit(double maskPointsPerUnit) {
        this.maskPointsPerUnit = maskPointsPerUnit;
    }

    double maskPointsPerUnit = 1;
    double texturePointsPerUnit = 0.2;
    double textureXShift = 0;
    double textureYShift = 0;

    int texturePatchWidth = 1024;
    int texturePatchHeight = 1024;

    private TexturedPatch(Coordinate[] vertexes, Collection<int[]> triangles) {
        this.vertexes = vertexes;
        this.triangles = triangles;

        vertexesToTextureCoordinates = new HashMap<>();

        HashSet<Integer> unique_vertex_ids = new HashSet<>();

        vertexCount = 0;
        triangleCount = 0;

        coordinatesEnvelope = new Envelope();
        for (int[] triangle : triangles) {
            for (int i = 0; i != 3; ++i) {
                unique_vertex_ids.add(triangle[i]);
                coordinatesEnvelope.expandToInclude(vertexes[triangle[i]]);
            }
            triangleCount +=1;
        }

        vertexCount = unique_vertex_ids.size();
    }

    boolean textureCoordsInitialized = false;
    private void initializeTextureCoordinates() {

        vertexesToTextureCoordinates.clear();

        if (textureCoordinates != null) textureCoordinates.clear();
        else {
            textureCoordinates = new ArrayList<>();}

        validateAspect();

        for (int[] triangle : triangles) {

            for (int i = 0; i != 3; ++i) {
                int vertex_id = triangle[i];
                Coordinate vertex = vertexes[vertex_id];
                Coordinate texture_coordinate = XYtoUV(vertex);
                int texture_coordinate_id = textureCoordinates.size();
                textureCoordinates.add(texture_coordinate);
                vertexesToTextureCoordinates.put(vertex_id,texture_coordinate_id);
            }

        }

        textureCoordsInitialized = true;
    }

    public Envelope getCoordinatesEnvelope() {
        return coordinatesEnvelope;
    }

    public Envelope getUvEnvelope() {
        return uvEnvelope;
    }

    public double getEdgePadding() {
        return edgePadding;
    }

    public void setEdgePadding(double edge_padding) {
        this.edgePadding = edge_padding;
        //validateAspect();
    }

    public boolean isMantainAspect() {
        return mantainAspect;
    }

    public void setMantainAspect(boolean mantain_aspect) {
        this.mantainAspect = mantain_aspect;
        //validateAspect();
    }

    public static String extendTextureName(String str, int index) {
        String extension = OutputUtils.getExtension(str);
        String file_name = str;
        if (extension.length() > 0) {
            file_name = str.substring(0, str.length() - extension.length()-1);
        }
        if (extension.length() == 0) {
            extension = "png";
        }
        return file_name+"_"+index+"."+extension;
    }

    public static String getDefaultTextureNameBase() {
        return "default_texture";
    }

    public static String getDefaultTextureExtension() {
        return "png";
    }

    private void validateAspect() {

        double width_padding = 0;
        double height_padding = 0;

        if (mantainAspect) {
            if (coordinatesEnvelope.getWidth() < coordinatesEnvelope.getHeight()) {
                width_padding = (coordinatesEnvelope.getHeight() - coordinatesEnvelope.getWidth()) / 2 / coordinatesEnvelope.getHeight();
            } else {
                height_padding = (coordinatesEnvelope.getWidth() - coordinatesEnvelope.getHeight()) / 2 / coordinatesEnvelope.getWidth();
            }
        }

        width_padding+= edgePadding;
        height_padding+= edgePadding;

        uvEnvelope = new Envelope(width_padding,1-width_padding,height_padding,1-height_padding);

    }

    public boolean isTextureCoordsInitialized() {
        return textureCoordsInitialized;
    }

    public int getTriangleCount() {
        return triangleCount;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public double getWidth() {
        return coordinatesEnvelope.getWidth();
    }

    public double getHeight() {
        return coordinatesEnvelope.getHeight();
    }

    public Coordinate UVtoXY(Coordinate uv) {
        Coordinate result = new Coordinate();
        UVtoXY(uv.x, uv.y, result);
        return result;
    }

    public Coordinate UVtoXY(double u, double v) {
        Coordinate ret = new Coordinate();
        UVtoXY(u,v,ret);
        return ret;
    }


    public void UVtoXY(Coordinate uv, Coordinate result) {
        UVtoXY(uv.x, uv.y, result);
    }

    public void UVtoXY(double u, double v, Coordinate result) {
        result.x = GeomUtils.map(u, uvEnvelope.getMinX(), uvEnvelope.getMaxX(), coordinatesEnvelope.getMinX(), coordinatesEnvelope.getMaxX());
        result.y = GeomUtils.map(v, uvEnvelope.getMaxY(), uvEnvelope.getMinY(), coordinatesEnvelope.getMinY(), coordinatesEnvelope.getMaxY());
    }

    public Coordinate XYtoUV(Coordinate xy) {
        Coordinate ret = new Coordinate();
        XYtoUV(xy.x,xy.y,ret);
        return ret;
    }


    public void XYtoUV(Coordinate xy, Coordinate result) {
        XYtoUV(xy.x, xy.y, result);
    }

    public Coordinate XYtoUV(double x, double y) {
        Coordinate ret = new Coordinate();
        XYtoUV(x,y,ret);
        return ret;
    }

    public void XYtoUV(double x, double y, Coordinate ret) {
        ret.x = GeomUtils.map(x, coordinatesEnvelope.getMinX(), coordinatesEnvelope.getMaxX(), uvEnvelope.getMinX(), uvEnvelope.getMaxX());
        ret.y = GeomUtils.map(y, coordinatesEnvelope.getMinY(), coordinatesEnvelope.getMaxY(), uvEnvelope.getMaxY(), uvEnvelope.getMinY());
    }

    public ArrayList<Coordinate> getTextureCoordinates() {
        return textureCoordinates;
    }

    public int getTextureCoordinateID(int vertex_id) {
        return vertexesToTextureCoordinates.get(vertex_id);
    }

    public PointRasterizer getTextureRasterizer() {

        Envelope squareEnvelope = new Envelope( UVtoXY( new Coordinate(0,0)), UVtoXY(new Coordinate(1,1)) );
        return new PointRasterizer(getTexturePatchWidth(), getTexturePatchHeight(), squareEnvelope);

    }

    public PointRasterizer getMaskRasterizer() {

        Envelope squareEnvelope = new Envelope( UVtoXY( new Coordinate(0,0)), UVtoXY(new Coordinate(1,1)) );

        PointRasterizer rasterizer = getTextureRasterizer();

        Envelope textureToCoordinatesEnvelope = new Envelope(
                rasterizer.toX(0), rasterizer.toX(rasterizer.getColumnCount()-1),
                rasterizer.toY(0), rasterizer.toY(rasterizer.getRowCount()-1)
        );

        int layerWidth = (int)Math.ceil( textureToCoordinatesEnvelope.getWidth() * getMaskPointsPerUnit() );
        int layerHeight = (int)Math.ceil( textureToCoordinatesEnvelope.getHeight() * getMaskPointsPerUnit()  );


        return new PointRasterizer(layerWidth, layerHeight, squareEnvelope);
    }

    private Pair<TexturedPatch,TexturedPatch> splitVerticallyInternal() {
        Collection<int[]> triangles1 = new ArrayList<>();
        Collection<int[]> triangles2 = new ArrayList<>();
        double x_threshold = (coordinatesEnvelope.getMinX()+ coordinatesEnvelope.getMaxX())*0.5;
        for (int[] triangle : triangles) {
            double center_x = TriangleUtils.getCenterX(vertexes,triangle);
            if (center_x < x_threshold)  {
                triangles1.add(triangle);
            } else {
                triangles2.add(triangle);
            }
        }

        TexturedPatch out_tp1 = new TexturedPatch(vertexes,triangles1);
        TexturedPatch out_tp2 = new TexturedPatch(vertexes,triangles2);

        return new Pair<>(out_tp1,out_tp2);
    }

    private Pair<TexturedPatch,TexturedPatch> splitHorizontallyInternal() {
        Collection<int[]> triangles1 = new ArrayList<>();
        Collection<int[]> triangles2 = new ArrayList<>();

        double y_threshold = (coordinatesEnvelope.getMinY()+ coordinatesEnvelope.getMaxY())*0.5;
        for (int[] triangle : triangles) {
            double center_y = TriangleUtils.getCenterY(vertexes,triangle);
            if (center_y < y_threshold) {
                triangles1.add(triangle);
            } else {
                triangles2.add(triangle);
            }
        }

        TexturedPatch out_tp1 = new TexturedPatch(vertexes,triangles1);
        TexturedPatch out_tp2 = new TexturedPatch(vertexes,triangles2);

        return new Pair<>(out_tp1,out_tp2);
    }


    private Pair<TexturedPatch,TexturedPatch> splitInternal() {
        if (coordinatesEnvelope.getMaxX()- coordinatesEnvelope.getMinX() > coordinatesEnvelope.getMaxY()- coordinatesEnvelope.getMinY()) {
            return splitVerticallyInternal();
        } else {
            return splitHorizontallyInternal();
        }
    }

    private static final Integer max_split_times = 10;
    public static void splitRecursivly(TexturedPatch initial, Collection<TexturedPatch> out, double max_width, double max_height,
                                double max_area, int max_triangle_count, int max_vertex_count, boolean mantain_aspect) {

        ArrayDeque<Integer> splitted_times = new ArrayDeque<>();
        ArrayDeque<TexturedPatch> split_queue = new ArrayDeque<>();
        split_queue.add(initial);
        splitted_times.add(0);

        while (split_queue.size() > 0) {
            TexturedPatch tp = split_queue.poll();
            int splits = splitted_times.poll();

            if (splits > max_split_times) {
                if (!tp.isTextureCoordsInitialized()) tp.initializeTextureCoordinates();
                out.add(tp);
                continue;
            }

            if ((max_width > 0 && tp.getWidth() > max_width) ||
                    (max_height > 0 && tp.getHeight() > max_height) ||
                    (max_area > 0 && tp.getHeight()*tp.getWidth() > max_area) ||
                    (max_triangle_count > 0 && tp.getTriangleCount() > max_triangle_count) ||
                    (max_vertex_count > 0 && tp.getVertexCount() > max_vertex_count)) {
                Pair<TexturedPatch,TexturedPatch> new_patches = tp.splitInternal();

                new_patches.v1.setEdgePadding(tp.getEdgePadding());
                new_patches.v1.setMantainAspect(tp.isMantainAspect());

                new_patches.v2.setEdgePadding(tp.getEdgePadding());
                new_patches.v2.setMantainAspect(tp.isMantainAspect());

                split_queue.add(new_patches.v1);
                splitted_times.add(splits+1);
                split_queue.add(new_patches.v2);
                splitted_times.add(splits+1);
            } else {
                if (mantain_aspect && (tp.getWidth()/tp.getHeight() > 1.6 || tp.getHeight()/tp.getWidth() > 1.6)) {
                    Pair<TexturedPatch,TexturedPatch> new_patches = tp.splitInternal();
                    if (!new_patches.v1.isTextureCoordsInitialized()) {
                        new_patches.v1.setEdgePadding(tp.getEdgePadding());
                        new_patches.v1.setMantainAspect(tp.isMantainAspect());
                        new_patches.v1.initializeTextureCoordinates();
                    }
                    if (!new_patches.v2.isTextureCoordsInitialized()) {
                        new_patches.v2.setEdgePadding(tp.getEdgePadding());
                        new_patches.v2.setMantainAspect(tp.isMantainAspect());
                        new_patches.v2.initializeTextureCoordinates();
                    }

                    out.add(new_patches.v1);
                    out.add(new_patches.v2);
                } else {
                    if (!tp.isTextureCoordsInitialized()) tp.initializeTextureCoordinates();
                    out.add(tp);
                }
            }
        }
    }

    public static TexturedPatch fromTriangles(Coordinate[] coords, Collection<int[]> triangles) {
        TexturedPatch tp = new TexturedPatch(coords,triangles);
        tp.initializeTextureCoordinates();
        return tp;
    }


}
