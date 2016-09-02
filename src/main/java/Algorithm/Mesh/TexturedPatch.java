package Algorithm.Mesh;

import Utils.GeomUtils;
import Utils.Pair;
import Utils.PointRasterizer;
import Utils.TriangleUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import java.util.*;

/**
 * Created by Artyom.Fomenko on 31.08.2016.
 */
public class TexturedPatch {

    ArrayList<Coordinate> texture_coordinates;
    Coordinate[] vertexes;
    Collection<int[]> triangles;

    HashMap<Integer,Integer> vertexes_to_textureCoordinates;

    Envelope coords_envelope;
    Envelope uv_envelope;

    int vertex_count;
    int triangle_count;

    double edge_padding;
    boolean mantain_aspect;

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
        return texturePatchWidth;
    }

    public void setTexturePatchWidth(int texturePatchWidth) {
        this.texturePatchWidth = texturePatchWidth;
    }

    public int getTexturePatchHeight() {
        return texturePatchHeight;
    }

    public void setTexturePatchHeight(int texturePatchHeight) {
        this.texturePatchHeight = texturePatchHeight;
    }

    public double getMaskPointsPerUnit() {
        return maskPointsPerUnit;
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

        vertexes_to_textureCoordinates = new HashMap<>();

        HashSet<Integer> unique_vertex_ids = new HashSet<>();

        vertex_count = 0;
        triangle_count = 0;

        coords_envelope = new Envelope();
        for (int[] triangle : triangles) {
            for (int i = 0; i != 3; ++i) {
                unique_vertex_ids.add(triangle[i]);
                coords_envelope.expandToInclude(vertexes[triangle[i]]);
            }
            triangle_count+=1;
        }

        vertex_count = unique_vertex_ids.size();
    }

    boolean textureCoordsInitialized = false;
    private void initializeTextureCoordinates() {

        vertexes_to_textureCoordinates.clear();

        if (texture_coordinates != null) texture_coordinates.clear();
        else {texture_coordinates = new ArrayList<>();}

        validateAspect();

        for (int[] triangle : triangles) {

            for (int i = 0; i != 3; ++i) {
                int vertex_id = triangle[i];
                Coordinate vertex = vertexes[vertex_id];
                Coordinate texture_coordinate = XYtoUV(vertex);
                int texture_coordinate_id = texture_coordinates.size();
                texture_coordinates.add(texture_coordinate);
                vertexes_to_textureCoordinates.put(vertex_id,texture_coordinate_id);
            }

        }

        textureCoordsInitialized = true;
    }

    public Envelope getCoordsEnvelope() {
        return coords_envelope;
    }

    public Envelope getUvEnvelope() {
        return uv_envelope;
    }

    public double getEdgePadding() {
        return edge_padding;
    }

    public void setEdgePadding(double edge_padding) {
        this.edge_padding = edge_padding;
        //validateAspect();
    }

    public boolean isMantainAspect() {
        return mantain_aspect;
    }

    public void setMantainAspect(boolean mantain_aspect) {
        this.mantain_aspect = mantain_aspect;
        //validateAspect();
    }

    private void validateAspect() {

        double width_padding = 0;
        double height_padding = 0;

        if (mantain_aspect) {
            if (coords_envelope.getWidth() < coords_envelope.getHeight()) {
                width_padding = (coords_envelope.getHeight() - coords_envelope.getWidth()) / 2 / coords_envelope.getHeight();
            } else {
                height_padding = (coords_envelope.getWidth() - coords_envelope.getHeight()) / 2 / coords_envelope.getWidth();
            }
        }

        width_padding+=edge_padding;
        height_padding+=edge_padding;

        uv_envelope = new Envelope(width_padding,1-width_padding,height_padding,1-height_padding);

    }

    public boolean isTextureCoordsInitialized() {
        return textureCoordsInitialized;
    }

    public int getTriangleCount() {
        return triangle_count;
    }

    public int getVertexCount() {
        return vertex_count;
    }

    public double getWidth() {
        return coords_envelope.getWidth();
    }

    public double getHeight() {
        return coords_envelope.getHeight();
    }

    public Coordinate UVtoXY(Coordinate uv) {
        Coordinate ret = new Coordinate();
        UVtoXY(uv.x,uv.y,ret);
        return ret;
    }

    public Coordinate UVtoXY(double u, double v) {
        Coordinate ret = new Coordinate();
        UVtoXY(u,v,ret);
        return ret;
    }


    public void UVtoXY(Coordinate uv, Coordinate ret) {
        UVtoXY(uv.x,uv.y,ret);
    }

    public void UVtoXY(double u, double v, Coordinate ret) {
        ret.x = GeomUtils.map(u,uv_envelope.getMinX(),uv_envelope.getMaxX(),coords_envelope.getMinX(),coords_envelope.getMaxX());
        ret.y = GeomUtils.map(v,uv_envelope.getMaxY(),uv_envelope.getMinY(),coords_envelope.getMinY(),coords_envelope.getMaxY());
    }

    public Coordinate XYtoUV(Coordinate xy) {
        Coordinate ret = new Coordinate();
        XYtoUV(xy.x,xy.y,ret);
        return ret;
    }


    public void XYtoUV(Coordinate xy, Coordinate ret) {
        XYtoUV(xy.x,xy.y,ret);
    }

    public Coordinate XYtoUV(double x, double y) {
        Coordinate ret = new Coordinate();
        XYtoUV(x,y,ret);
        return ret;
    }

    public void XYtoUV(double x, double y, Coordinate ret) {
        ret.x = GeomUtils.map(x,coords_envelope.getMinX(),coords_envelope.getMaxX(),uv_envelope.getMinX(),uv_envelope.getMaxX());
        ret.y = GeomUtils.map(y,coords_envelope.getMinY(),coords_envelope.getMaxY(),uv_envelope.getMaxY(),uv_envelope.getMinY());
    }

    public ArrayList<Coordinate> getTextureCoordinates() {
        return texture_coordinates;
    }

    public int getTextureCoordinateID(int vertex_id) {
        return vertexes_to_textureCoordinates.get(vertex_id);
    }

    public PointRasterizer getTextureRasterizer() {

        Envelope square_envelope = new Envelope( UVtoXY( new Coordinate(0,0)), UVtoXY(new Coordinate(1,1)) );
        return new PointRasterizer(getTexturePatchWidth(),getTexturePatchHeight(),square_envelope);

    }

    public PointRasterizer getMaskRasterizer() {

        Envelope square_envelope = new Envelope( UVtoXY( new Coordinate(0,0)), UVtoXY(new Coordinate(1,1)) );

        PointRasterizer rast = getTextureRasterizer();

        Envelope texture_to_coordinates_envelope = new Envelope(
                rast.toX(0),rast.toX(rast.getColumnCount()-1),
                rast.toY(0),rast.toY(rast.getRowCount()-1)
        );

        int layer_width = (int)Math.ceil( texture_to_coordinates_envelope.getWidth()*getMaskPointsPerUnit() );
        int layer_hegith = (int)Math.ceil( texture_to_coordinates_envelope.getHeight()*getMaskPointsPerUnit()  );


        return new PointRasterizer(layer_width,layer_hegith,square_envelope);
    }

    private Pair<TexturedPatch,TexturedPatch> splitVerticallyInternal() {
        Collection<int[]> triangles1 = new ArrayList<>();
        Collection<int[]> triangles2 = new ArrayList<>();
        double x_threshold = (coords_envelope.getMinX()+coords_envelope.getMaxX())*0.5;
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

        double y_threshold = (coords_envelope.getMinY()+coords_envelope.getMaxY())*0.5;
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
        if (coords_envelope.getMaxX()-coords_envelope.getMinX() > coords_envelope.getMaxY()-coords_envelope.getMinY()) {
            return splitVerticallyInternal();
        } else {
            return splitHorizontallyInternal();
        }
    }

    public static void splitRecursivly(TexturedPatch initial, Collection<TexturedPatch> out, double max_width, double max_height,
                                double max_area, int max_triangle_count, int max_vertex_count, boolean mantain_aspect) {

        ArrayDeque<TexturedPatch> split_queue = new ArrayDeque<>();
        split_queue.add(initial);

        while (split_queue.size() > 0) {
            TexturedPatch tp = split_queue.poll();

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
                split_queue.add(new_patches.v2);
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

//    public static void fromTrianglesSplit(Coordinate[] coords, Collection<int[]> triangles,
//                                          Collection<TexturedPatch> patches_out,
//                                          double max_width, double max_height,
//                                          double max_area, int max_triangle_count, int max_vertex_count, boolean mantain_aspect) {
//        TexturedPatch initial = fromTriangles(coords,triangles);
//
//        splitRecursivly(initial,patches_out,max_width,max_height,max_area,max_triangle_count,max_vertex_count,mantain_aspect);
//    }

}
