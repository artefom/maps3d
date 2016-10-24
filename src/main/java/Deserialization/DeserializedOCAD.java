package Deserialization;

import Deserialization.Binary.*;
import Deserialization.Interpolation.SlopeMark;
import com.vividsolutions.jts.geom.*;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.Isoline;
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

    private ArrayList<TOcadObject> objects;
    private HashMap< Integer,ArrayList<TRecord> > records;
    public Set<Integer> symbol_ids;
    private double metersPerUnit;

    public void DeserializeMap(File ocadFile, BiConsumer<Integer, Integer> progressUpdate) throws Exception {
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

        objects = new ArrayList<>(2048);
        records = new HashMap<>(2048);
        symbol_ids = new HashSet<>();

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
                            if (!records.containsKey(rec.getTypeID())) records.put(rec.getTypeID(),new ArrayList<>());
                            records.get(rec.getTypeID()).add(rec);
                        }
                    }
                }
                progress += stringIB.Table.length;
                updateProgress(progressUpdate, progress, total);
            }
        }
        ArrayList<TRecord> scaleParameters = getRecordsByName("ScalePar");
        if (scaleParameters.size() != 1) throw new RuntimeException("File's map scale not understood");
        this.metersPerUnit = scaleParameters.get(0).getValueAsDouble('m')/1000;
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

                objects.add(obj);
                symbol_ids.add(obj.Sym);
                updateProgress(progressUpdate, progress, total);
            }
        }
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

    public ArrayList<IIsoline> toIsolines(double interpolationStep ,GeometryFactory gf) throws Exception {
        if (interpolationStep <= 0) throw new Exception("Invalid interpolation step");

        slopeMarks = new ArrayList<>();
        slopeMarks.addAll(objects.stream().filter(TOcadObject::isSlope).map(SlopeMark::new).collect(Collectors.toList()));

        ArrayList<IIsoline> ret = new ArrayList<>();
        for (TOcadObject obj : objects) {
            if (obj.isLine()) {

                // Detected slope, lying on this curve;
                SlopeMark slope = null;
                LineString ls = CurveString.fromOcadVertices(obj.vertices).interpolate(gf);

                // Find slope within specified distance
                for (SlopeMark s : slopeMarks) {
                    Point p = gf.createPoint(s.origin);
                    if (ls.isWithinDistance(p, Constants.slope_near_dist)) {
                        slope = s;
                        break;
                    }
                }

                int slopeSide = 0;
                if (slope != null) {
                    double precision = Constants.tangent_precision / ls.getLength();
                    double projectionFactor = GeomUtils.projectionFactor(slope.origin, ls);
                    double pos1 = GeomUtils.clamp(projectionFactor-precision, 0, 1);
                    double pos2 = GeomUtils.clamp(projectionFactor+precision, 0, 1);
                    Coordinate c1 = GeomUtils.pointAlong(ls, pos1);
                    Coordinate c2 = GeomUtils.pointAlong(ls, pos2);
                    LineSegment seg = new LineSegment(c1, c2);
                    slopeSide = GeomUtils.getSide(seg, slope.pointAlong(Constants.slope_length));
                }
                if (ls.getLength() < 0.01) {
                    System.out.println("Too small line string, skip");
                } else if  (ls.getNumPoints() < 2) {
                    System.out.println("Invalid line string, skip");
                } else {
                    ret.add(new Isoline(obj.getType(), slopeSide, ls.getCoordinateSequence(), gf));
                }
            }
        }

        return ret;
    }

    public List<TOcadObject> getObjectsByID(int symbolId) {
        return objects.stream().filter(obj -> obj.Sym == symbolId).collect(Collectors.toList());
    }

    public List<TOcadObject> getObjectsByIDs(List<Integer> symbolIds) {
        return objects.stream().filter(obj -> symbolIds.contains(obj.Sym)).collect(Collectors.toList());
    }

    private static List<String> splitMask(String mask) {
        List<String> result = Arrays.asList(mask.split(","));
        for (int i = 0; i != result.size(); ++i) {
            result.set(i,result.get(i).trim());
        }
        return result;
    }

    private static boolean matchesMask(int symbolId, String mask) {
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

    private static boolean matchesMasks(int symbolId, List<String> masks) {
        return masks.stream().anyMatch(obj -> matchesMask(symbolId, obj));
    }

    public List<TOcadObject> getObjectsByMask(String mask) {
        List<String> masks = splitMask(mask);
        return objects.stream().filter(obj -> matchesMasks(obj.Sym, masks)).collect(Collectors.toList());
    }
}
