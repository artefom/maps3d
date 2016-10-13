package ru.ogpscenter.maps3d.isolines;

import com.google.gson.*;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.*;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;
import ru.ogpscenter.maps3d.utils.Interpolator;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * provides container for ru.ogpscenter.maps3d.isolines,
 * high-level geometry operations,
 * isoline indexing
 */
public class IsolineContainer extends HashSet<IIsoline> {

    private GeometryFactory gf;

    public IsolineContainer(GeometryFactory gf, Stream<IIsoline> stream) {
        super((int)stream.count());
        this.gf = gf;
        stream.forEach(this::add);
    }

    public IsolineContainer(GeometryFactory gf, Collection<IIsoline> isolines) {
        super(isolines.size());
        this.gf = gf;
        isolines.forEach(this::add);
    }


    public IsolineContainer(GeometryFactory gf) {
        super();
        this.gf = gf;
    }

    public IsolineContainer(IsolineContainer other) {
        super(other.size());
        this.gf = other.gf;
        other.forEach((x) -> this.add(new Isoline(x)));
    }

    /**
     * @return Bounding box of all ru.ogpscenter.maps3d.isolines (eg. usage: fitting view to whole map)
     */
    public Envelope getEnvelope() {
        Envelope e = new Envelope();
        this.forEach( (v) -> e.expandToInclude( v.getGeometry().getEnvelopeInternal() ) );
        return e;
    }

    /**
     * Get convex hull of the map
     */
    public ConvexHull convexHull() {
        Coordinate[] points_list = this.stream().flatMap(
                (il)-> Arrays.stream(il.getGeometry().getCoordinates())).toArray(Coordinate[]::new);
        return new ConvexHull(points_list,gf);
    }

    public static List<Coordinate> getEndPointArray(LineString ls, int end_index, double offset, int iterations) {
        offset = offset/ls.getLength();
        double step_size = offset/iterations;

        if (offset > 0.3)
            offset = 0.3;

        if (end_index == 1)
            return Interpolator.InterpolateAlongLocal(ls,0,offset,step_size);
        else
            return Interpolator.InterpolateAlongLocal(ls,1,1-offset,step_size);
    }

    public GeometryFactory getFactory() {
        return gf;
    }

    public ArrayList<Geometry> getIsolinesAsGeometry() {
        ArrayList<Geometry> ret = new ArrayList<>(this.size());
        ret.addAll(this.stream().map(IIsoline::getGeometry).collect(Collectors.toList()));
        return ret;
    }

    public void serialize(String path) {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(LineString.class, new LineStringAdapter(gf));
        String result = gsonBuilder.create().toJson(this);
        try {
            PrintWriter writer = new PrintWriter(path, "UTF-8");
            writer.println(result);
            writer.close();
        } catch (Exception ex) {
            CommandLineUtils.printWarning("could not serialize Isoline Container, reason: "+ex.getMessage());
        }
    }

    public List<IIsoline> findInCircle(Coordinate center, double radius) {
        Point p = gf.createPoint( center );
        return this.stream().filter((il)->
                il.getGeometry().isWithinDistance(p,radius)
        ).collect(Collectors.toList());
    }

    public List<IIsoline> getIntersecting(LineString ls) {
        return this.stream().filter(iso -> iso.getLineString().intersects(ls)).collect(Collectors.toCollection(ArrayList::new));
    }


    public Optional<IIsoline> findClosest(Coordinate center) {
        Point p = gf.createPoint( center );
        return this.stream().min((lhs,rhs)-> Double.compare(p.distance(lhs.getGeometry()),p.distance(rhs.getGeometry())));
    }

    public static IsolineContainer deserialize(String path) throws Exception{

        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String json_str = new String(encoded, StandardCharsets.UTF_8);

        GeometryFactory gf = new GeometryFactory();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(IsolineContainer.class, new IsolineContainerAdapter(gf));
        Gson g = gsonBuilder.create();
        try {
            return g.fromJson(json_str, IsolineContainer.class);
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
            return null;
        }
    }

    public static class LineStringAdapter implements JsonSerializer<LineString>, JsonDeserializer<LineString> {

        GeometryFactory gf;

        public LineStringAdapter(GeometryFactory gf) {
            this.gf =gf;
        }

        @Override
        public JsonElement serialize(LineString src, Type typeOfSrc,
                                     JsonSerializationContext context)
        {
            JsonObject obj = new JsonObject();
            Coordinate[] coordinates = src.getCoordinates();
            JsonArray       x_array         = new JsonArray();
            JsonArray       y_array         = new JsonArray();

            for (int i = 0; i != coordinates.length; ++i) {
                x_array.add( new JsonPrimitive(coordinates[i].x));
                y_array.add( new JsonPrimitive(coordinates[i].y));
            }
            obj.add("xs",  x_array );
            obj.add("ys",  y_array );
            return obj;
        }

        @Override
        public LineString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject jobject = (JsonObject) json;

            JsonArray xs_json = jobject.get("xs").getAsJsonArray();
            JsonArray ys_json = jobject.get("ys").getAsJsonArray();

            Iterator<JsonElement> x_it = xs_json.iterator();
            Iterator<JsonElement> y_it = ys_json.iterator();

            ArrayList<Coordinate> coordinates = new ArrayList<>();

            while (x_it.hasNext() && y_it.hasNext()) {
                coordinates.add( new Coordinate(x_it.next().getAsDouble(),y_it.next().getAsDouble()));
            }
            return gf.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        }
    }

    private static class IsolineAdapter implements JsonDeserializer<Isoline> {

        GeometryFactory gf;
        IsolineAdapter(GeometryFactory gf) {
            this.gf = gf;
        }
        @Override
        public Isoline deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(LineString.class, new LineStringAdapter(gf));
            Gson g = gsonBuilder.create();

            JsonObject jobj = json.getAsJsonObject();

            LineString ls = g.fromJson(jobj.get("lineString"),LineString.class);
            int type = jobj.get("type").getAsInt();
            int slope_side = jobj.get("slope_side").getAsInt();
            int id = jobj.get("id").getAsInt();
            boolean edgeToEdge = jobj.get("isedgetoedge").getAsBoolean();
            double height = jobj.get("height").getAsDouble();

            Isoline new_isoline = new Isoline(type,slope_side,ls.getCoordinateSequence(),gf);

            new_isoline.setHeight(height);
            new_isoline.setEdgeToEdge(edgeToEdge);
            new_isoline.setID(id);
            return new_isoline;
        }
    }

    private static class IsolineContainerAdapter implements JsonDeserializer<IsolineContainer> {

        GeometryFactory gf;
        IsolineContainerAdapter(GeometryFactory gf) {
            this.gf = gf;
        }
        @Override
        public IsolineContainer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Isoline.class, new IsolineAdapter(gf));
            Gson g = gsonBuilder.create();

            JsonArray isoline_json_arr = json.getAsJsonArray();

            ArrayList<IIsoline> isolines = new ArrayList<>();

            for (JsonElement e : isoline_json_arr) {
                Isoline i = g.fromJson(e,Isoline.class);
                isolines.add(i);
            }

            return new IsolineContainer(gf,isolines);
        }
    }


}
