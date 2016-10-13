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
import java.util.function.Function;

/**
 * Created by Artem on 21.07.2016.
 */
public class DeserializedOCAD {

    private static final long MAX_OCAD_FILE_SIZE = 500 * 1024 * 1024;

    public DeserializedOCAD() {
    }

    public ArrayList<TOcadObject> objects;
    public HashMap< Integer,ArrayList<TRecord> > records;
    public Set<Integer> symbol_ids;
    private double metersPerUnit;


    public void DeserializeMap( String path, String configPath ) throws Exception {

        File ocadFile = new File(path);
        long ocadFileSize = ocadFile.length();
        if (ocadFileSize > MAX_OCAD_FILE_SIZE) {
            throw new Exception("File too big");
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
                    new Coordinate((double) ixy.v1 / Constants.map_scale_fix * metersPerUnit * PropertiesLoader.ocad_input.scale_multiplier
                            , (double) ixy.v2 / Constants.map_scale_fix * metersPerUnit * PropertiesLoader.ocad_input.scale_multiplier);
        } else {
            coordinateConverter = ixy ->
                    new Coordinate((double) ixy.v1 / Constants.map_scale_fix * PropertiesLoader.ocad_input.scale_multiplier ,
                            (double) ixy.v2 / Constants.map_scale_fix * PropertiesLoader.ocad_input.scale_multiplier);
        }

        int nextIndexBlock = header.ObjectIndexBlock;
        TObjectIndexBlock indexBlock = new TObjectIndexBlock();
        while(nextIndexBlock != 0) {

            indexBlock.deserialize(buffer,nextIndexBlock);

            nextIndexBlock = indexBlock.NextObjectIndexBlock;

            for (int i = 0; i != 256; ++i) {
                TObjectIndex oi = indexBlock.Table[i];
                if (oi.ObjType == 0) {
                    continue;
                }
                if (!(oi.ObjType > 0 && oi.ObjType <= 7 )) {
                    throw new Exception("Invalid format: invalid object type");
                }
                TOcadObject obj = new TOcadObject(coordinateConverter);
                obj.deserialize(buffer,oi.Pos);

                objects.add(obj);
                symbol_ids.add(obj.Sym);
            }
        }
    }

    public ArrayList<TRecord> getRecordsByName(String name) {
        return getRecordsByID( TRecord.recordIDs.getOrDefault(name,null) );
    };

    public ArrayList<TRecord> getRecordsByID(Integer id) {
        return records.getOrDefault(id,null);
    };

    public ArrayList<SlopeMark> slopeMarks;

    public ArrayList<IIsoline> toIsolines(double interpolation_step ,GeometryFactory gf) throws Exception {
        if (interpolation_step <= 0) throw new Exception("Invalid interpolation step");

        slopeMarks = new ArrayList<>();
        for (TOcadObject obj : objects) {
            if (obj.isSlope()) {
                slopeMarks.add( new SlopeMark(obj) );
            }
            //ret.add(CurveString.fromTDPoly(obj.Poly).interpolate(interpolation_step,gf));
        }

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

                int slope_side = 0;
                if (slope != null) {
                    double prec = Constants.tangent_precision/ls.getLength();
                    double projFact = GeomUtils.projectionFactor(slope.origin,ls);
                    double pos1 = GeomUtils.clamp(projFact-prec,0,1);
                    double pos2 = GeomUtils.clamp(projFact+prec,0,1);
                    Coordinate c1 = GeomUtils.pointAlong(ls,pos1);
                    Coordinate c2 = GeomUtils.pointAlong(ls,pos2);
                    LineSegment seg = new LineSegment(c1,c2);
                    slope_side = GeomUtils.getSide(seg,slope.pointAlong(Constants.slope_length));
                    // Find out slope side
                    //Coordinate endpoint = Vector2D.create(slope.origin).add(slope.vec.multiply(Constants.slope_length)).toCoordinate();
                    //Coordinate p1;
                }
                if (ls.getLength() < 0.01) {
                    System.out.println("Too small line string, skip");
                } else if  (ls.getNumPoints() < 2) {
                    System.out.println("Invalid line string, skip");
                } else {
                    ret.add(new Isoline(obj.getType(), slope_side, ls.getCoordinateSequence(), gf));
                }
            }
        }

        return ret;
    }

    public List<TOcadObject> getObjectsByID(int symbol_id) {
        List<TOcadObject> ret = new ArrayList<>();
        for (TOcadObject obj : objects) {
            if (obj.Sym == symbol_id) {
                ret.add(obj);
            }
        }
        return ret;
    }

    public List<TOcadObject> getObjectsByIDs(List<Integer> symbol_ids) {
        List<TOcadObject> ret = new ArrayList<>();
        for (TOcadObject obj : objects) {
            for (int i = 0; i != symbol_ids.size(); ++i) {
                if ( obj.Sym == symbol_ids.get(i) ) {
                    ret.add(obj);
                    break;
                }
            }
        }
        return ret;
    }

    private static List<String> splitMask(String mask) {
        List<String> ret = Arrays.asList(mask.split(","));
        for (int i = 0; i != ret.size(); ++i) {
            ret.set(i,ret.get(i).trim());
        }
        return ret;
    }

    private static boolean matchesMask(int symbol_id, String mask) {
        int correction = 0;
        boolean negate = false;
        if (mask.charAt(0) == '~') {
            negate = true;
            correction = 1;
        }
        String mask_2 = Integer.toString(symbol_id);

        boolean matches_all = true;
        for (int i = 0; i != mask_2.length(); ++i) {

            if ( mask_2.charAt(i) != mask.charAt(i+correction) ) {
                if (mask.charAt(i+correction) != '.') {
                    matches_all = false;
                    break;
                }
            }

        }
        if (matches_all) return true; return false;
    }

    public static boolean matchesMask(int symbol_id, List<String> masks) {
        boolean ret = false;
        for (String mask_part : masks) {
            //System.out.println(mask_part);
            if (mask_part.charAt(0) == '~') {
                if (matchesMask(symbol_id, mask_part)) {
                    return false;
                }
            } else {
                if (matchesMask(symbol_id, mask_part)) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    public List<TOcadObject> getObjectsByMask(String mask) {
        List<String> masks = splitMask(mask);

        List<TOcadObject> ret = new ArrayList<>();
        for (TOcadObject obj : objects) {
            if ( matchesMask(obj.Sym,masks) ) {
                ret.add(obj);
            }
        }
        return ret;
    }


}
