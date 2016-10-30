package Deserialization;

import Deserialization.Binary.*;
import Deserialization.Interpolation.SlopeMark;
import com.vividsolutions.jts.geom.*;
import org.jetbrains.annotations.NotNull;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.Isoline;
import ru.ogpscenter.maps3d.isolines.SlopeSide;
import ru.ogpscenter.maps3d.utils.Constants;
import ru.ogpscenter.maps3d.utils.GeomUtils;
import ru.ogpscenter.maps3d.utils.Pair;
import ru.ogpscenter.maps3d.utils.curves.CurveString;
import ru.ogpscenter.maps3d.utils.properties.PropertiesLoader;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Artem on 21.07.2016.
 *
 */
public class DeserializedOCAD {

    private static final long MAX_OCAD_FILE_SIZE = 500 * 1024 * 1024;

    public DeserializedOCAD() {
    }

    private HashMap< Integer,ArrayList<TRecord> > records;
    private HashMap<Integer, ArrayList<TOcadObject>> symbolIds2objects;

    private double metersPerUnit;
    public void loadOcad(File ocadFile, BiConsumer<Integer, Integer> progressUpdate) throws Exception {
        long ocadFileSize = ocadFile.length();
        if (ocadFileSize > MAX_OCAD_FILE_SIZE) {
            throw new Exception("OCAD File is too big. Must be < " + MAX_OCAD_FILE_SIZE + " bytes");
        }
        FileChannel fileChannel = new RandomAccessFile(ocadFile, "r").getChannel();
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) ocadFileSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        OcadHeader header = new OcadHeader();
        header.deserialize(buffer,0);
        if ( header.OCADMark != 3245 ) throw new Exception("Invalid format: wrong OCAD header mark");
        if ( header.Version != 11 ) throw new Exception("Invalid format: version 11 required");

        records = new HashMap<>(2048);
        symbolIds2objects = new HashMap<>();

        int nextStringIB = header.FirstStringIndexBlk;
        TStringIndexBlock stringIB = new TStringIndexBlock();
        int total = 0;
        // calculate total progress
        while (nextStringIB != 0) {
            stringIB.deserialize(buffer,nextStringIB);
            nextStringIB  = stringIB.NextIndexBlock;
            total += stringIB.Table.length;
        }
        int nextIndexBlock = header.ObjectIndexBlock;
        TObjectIndexBlock indexBlock = new TObjectIndexBlock();
        while(nextIndexBlock != 0) {
            indexBlock.deserialize(buffer,nextIndexBlock);
            nextIndexBlock = indexBlock.NextObjectIndexBlock;
            total += 256;
        }
        updateProgress(progressUpdate, 0, total);

        int progress = 0;
        nextStringIB = header.FirstStringIndexBlk;
        stringIB = new TStringIndexBlock();
        while (nextStringIB != 0) {
            stringIB.deserialize(buffer,nextStringIB);
            nextStringIB  = stringIB.NextIndexBlock;
            for (int i = 0; i != stringIB.Table.length; ++i) {
                if (stringIB.Table[i] != null && stringIB.Table[i].getString() != null) {
                    if (stringIB.Table[i] != null && stringIB.Table[i].getString().length() != 0) {
                        TRecord rec =TRecord.fromTStringIndex(stringIB.Table[i]);
                        if (rec != null && rec.isValid()) {
                            if (!records.containsKey(rec.getTypeID())) {
                                records.put(rec.getTypeID(),new ArrayList<>());
                            }
                            records.get(rec.getTypeID()).add(rec);
                        }
                    }
                }
                progress += stringIB.Table.length;
                updateProgress(progressUpdate, progress, total);
            }
        }
        ArrayList<TRecord> scaleParameters = getRecordsByName("ScalePar");
        if (scaleParameters.size() != 1) {
            throw new RuntimeException("File's map scale not understood");
        }
        this.metersPerUnit = scaleParameters.get(0).getValueAsDouble('m') / 1000;
        PropertiesLoader.update();
        Function<Pair<Integer, Integer>, Coordinate> coordinateConverter;
        if (PropertiesLoader.ocad_input.multiply_by_scale) {
            System.out.println("Meters per unit: "+this.metersPerUnit);
            coordinateConverter = ixy ->
                    new Coordinate(
                        (double) ixy.v1 / Constants.map_scale_fix * metersPerUnit * PropertiesLoader.ocad_input.scale_multiplier,
                        (double) ixy.v2 / Constants.map_scale_fix * metersPerUnit * PropertiesLoader.ocad_input.scale_multiplier
                    );
        } else {
            coordinateConverter = ixy ->
                    new Coordinate(
                        (double) ixy.v1 / Constants.map_scale_fix * PropertiesLoader.ocad_input.scale_multiplier,
                        (double) ixy.v2 / Constants.map_scale_fix * PropertiesLoader.ocad_input.scale_multiplier
                    );
        }

        nextIndexBlock = header.ObjectIndexBlock;
        indexBlock = new TObjectIndexBlock();
        while(nextIndexBlock != 0) {
            indexBlock.deserialize(buffer,nextIndexBlock);
            nextIndexBlock = indexBlock.NextObjectIndexBlock;
            for (int i = 0; i != 256; ++i) {
                TObjectIndex oi = indexBlock.Table[i];
                if (oi.ObjType == 0) {
                    // stop process block on 0 object type
                    break;
                }
                // 1 = Point object
                // 2 = Line object
                // 3 = Area object
                // 4 = Unformatted text
                // 5 = Formatted text
                // 6 = Line text
                // 7 = Rectangle object
                if (oi.ObjType < 1 || oi.ObjType > 7) {
                    // do not process unknown types
                    continue;
                }
                // 0 = deleted (not undo) (eg from symbol editor or course setting)
                // 1 = normal
                // 2 = hidden
                // 3 = deleted for undo
                if (oi.Status != 1) {
                    // do not process objects if State not equals to 'normal'
                    continue;
                }
                TOcadObject obj = new TOcadObject(coordinateConverter);
                obj.deserialize(buffer,oi.Pos);
                ArrayList<TOcadObject> objectsWithSymbol = symbolIds2objects.get(obj.Sym);
                if (objectsWithSymbol == null) {
                    objectsWithSymbol =  new ArrayList<>();
                    symbolIds2objects.put(obj.Sym, objectsWithSymbol);
                }
                objectsWithSymbol.add(obj);
                updateProgress(progressUpdate, progress, total);
            }
        }

        @NotNull List<TOcadObject> borders = getObjectsByMask(PropertiesLoader.ocad_input.border);
        border = borders.isEmpty() ? null : borders.get(0);
        if (borders.size() > 1) {
            System.out.println("Several border objects found! Will use first one");
        }
    }

    public HashMap<Integer, ArrayList<TOcadObject>> getSymbolIds2objects() {
        return symbolIds2objects;
    }

    private void updateProgress(BiConsumer<Integer, Integer> progressUpdate, int progress, int total) {
        if (progressUpdate != null) {
            progressUpdate.accept(progress, total);
        }
    }

    private ArrayList<TRecord> getRecordsByName(String name) {
        return getRecordsByID( TRecord.recordIDs.getOrDefault(name,null) );
    }

    private ArrayList<TRecord> getRecordsByID(Integer id) {
        return records.getOrDefault(id,null);
    }

    public ArrayList<SlopeMark> slopeMarks;
    public TOcadObject border;

    public ArrayList<IIsoline> toIsolines(GeometryFactory gf) throws Exception {
        slopeMarks = new ArrayList<>();
        slopeMarks.addAll(getObjectsByMask(PropertiesLoader.ocad_input.slope).stream().map(SlopeMark::new).collect(Collectors.toList()));

        ArrayList<IIsoline> result = new ArrayList<>();
        symbolIds2objects.values().forEach(objects -> {
            for (TOcadObject obj : objects) {
                if (obj.isLine()) {

                    // Detected slope, lying on this curve;
                    try {
                        SlopeMark slope = null;
                        LineString lineString = CurveString.fromOcadVertices(obj.vertices).interpolate(gf);

                        // Find slope within specified distance
                        for (SlopeMark slopeMark : slopeMarks) {
                            Point slopeMarkOrigin = gf.createPoint(slopeMark.origin);
                            if (lineString.isWithinDistance(slopeMarkOrigin, Constants.slope_near_dist)) {
                                slope = slopeMark;
                                break;
                            }
                        }

                        SlopeSide slopeSide = SlopeSide.NONE;
                        if (slope != null) {
                            double precision = Constants.tangent_precision / lineString.getLength();
                            double projectionFactor = GeomUtils.projectionFactor(slope.origin, lineString);
                            double pos1 = GeomUtils.clamp(projectionFactor - precision, 0, 1);
                            double pos2 = GeomUtils.clamp(projectionFactor + precision, 0, 1);
                            Coordinate c1 = GeomUtils.pointAlong(lineString, pos1);
                            Coordinate c2 = GeomUtils.pointAlong(lineString, pos2);
                            LineSegment segment = new LineSegment(c1, c2);
                            slopeSide = GeomUtils.getSide(segment, slope.pointAlong(Constants.slope_length));
                        }
                        if (lineString.getLength() < 0.01) {
                            System.out.println("Too small line string, skip");
                        } else if  (lineString.getNumPoints() < 2) {
                            System.out.println("Invalid line string, skip");
                        } else {
                            result.add(new Isoline(obj.getType(), slopeSide, lineString.getCoordinateSequence(), gf));
                        }
                    }
                    catch (Exception e) {
                        System.out.println("Error while processing object: " + e.getMessage());
                    }
                }
            }
        });
        return result;
    }

    public List<TOcadObject> getObjectsBySymbolId(int symbolId) {
        ArrayList<TOcadObject> objects = symbolIds2objects.get(symbolId);
        if (objects == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(objects);
    }

    public List<TOcadObject> getObjectsBySymbolIds(List<Integer> symbolIds) {
        ArrayList<TOcadObject> result =  new ArrayList<>();
        symbolIds.forEach(symbolId -> result.addAll(getObjectsBySymbolId(symbolId)));
        return result;
    }

    private static List<String> splitMask(String mask) {
        List<String> result = Arrays.asList(mask.split(","));
        for (int i = 0; i != result.size(); ++i) {
            result.set(i,result.get(i).trim());
        }
        return result;
    }

    public static boolean matchesMask(int symbolId, String mask) {
        String symbolIdString = Integer.toString(symbolId);
        int symbolIdLength = symbolIdString.length();
        if (symbolIdLength != mask.length()) {
            return false;
        }
        for (int i = 0; i != symbolIdLength; ++i) {
            if (symbolIdString.charAt(i) != mask.charAt(i) ) {
                if (mask.charAt(i) != '.') {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean matchesMasks(int symbolId, List<String> masks) {
        return masks.stream().anyMatch(obj -> matchesMask(symbolId, obj));
    }

    @NotNull
    public List<TOcadObject> getObjectsByMask(@NotNull String mask) {
        ArrayList<TOcadObject> result = new ArrayList<>();
        List<String> masks = splitMask(mask);
        symbolIds2objects.keySet().stream().filter(symbolId -> matchesMasks(symbolId, masks)).forEach(it -> {
            ArrayList<TOcadObject> objects = symbolIds2objects.get(it);
            if (objects != null) {
                result.addAll(objects);
            }
        });
        return result;
    }
}
