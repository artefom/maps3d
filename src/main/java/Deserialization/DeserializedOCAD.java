package Deserialization;

import Algorithm.LineConnection.Connection;
import Algorithm.LineConnection.Isoline_attributed;
import Algorithm.LineConnection.LineConnector;
import Algorithm.LineConnection.LineEnd;
import Isolines.IIsoline;
import Isolines.Isoline;
import Deserialization.Binary.*;
import Utils.CommandLineUtils;
import Utils.Curves.CurveString;
import Deserialization.Interpolation.SlopeMark;
import Utils.Constants;
import Utils.GeomUtils;
import Utils.Pair;
import Utils.Properties.PropertiesLoader;
import com.sun.corba.se.impl.orb.ORBConfiguratorImpl;
import com.sun.prism.impl.Disposer;
import com.vividsolutions.jts.geom.*;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.DoubleFunction;
import java.util.function.Function;

/**
 * Created by Artem on 21.07.2016.
 */
public class DeserializedOCAD {

    public DeserializedOCAD() {
    }

    public ArrayList<TOcadObject> objects;
    public HashMap< Integer,ArrayList<TRecord> > records;
    public Set<Integer> symbol_ids;
    private double metersPerUnit;

    public void DeserializeMap( String path, String configPath ) throws Exception {

        SeekableByteChannel ch = Files.newByteChannel(Paths.get(path)); // Defaults to read-only
        ch.position(0);

        OcadHeader header = new OcadHeader();
        header.Deserialize(ch,0);
        if ( header.OCADMark != 3245 ) throw new Exception("invalid format");
        if ( header.Version != 11 ) throw new Exception("invalid format");


        objects = new ArrayList<>(2048);
        records = new HashMap<>(2048);
        symbol_ids = new HashSet<>();


        int nextStringIB = header.FirstStringIndexBlk;
        TStringIndexBlock stringIB = new TStringIndexBlock();
        while (nextStringIB != 0) {
            stringIB.Deserialize(ch,nextStringIB);
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

        int nextib = header.ObjectIndexBlock;
        TObjectIndexBlock ib = new TObjectIndexBlock();
        while(nextib != 0) {

            ib.Deserialize(ch,nextib);

            nextib = ib.NextObjectIndexBlock;

            for (int i = 0; i != 256; ++i) {
                TObjectIndex oi = ib.Table[i];
                if (!( oi.ObjType >= 0 && oi.ObjType <= 7 )) throw new Exception("invalid format");
                TOcadObject obj = new TOcadObject(coordinateConverter);
                obj.Deserialize(ch,oi.Pos);

                objects.add(obj);
                symbol_ids.add(obj.Sym);
            };
        };


        return;
    }

    public ArrayList<TRecord> getRecordsByName(String name) {
        return getRecordsByID( TRecord.recordIDs.getOrDefault(name,null) );
    };

    public ArrayList<TRecord> getRecordsByID(Integer id) {
        return records.getOrDefault(id,null);
    };

    public ArrayList<SlopeMark> slopeMarks;

    private void weldEnds(LineString ls1, LineString ls2, int le1, int le2) {
        CoordinateSequence cs1 = ls1.getCoordinateSequence();
        CoordinateSequence cs2 = ls2.getCoordinateSequence();

        int index1 = le1 == 1 ? 0 : cs1.size()-1;
        int index2 = le2 == 1 ? 0 : cs2.size()-1;
        double new_x = (cs1.getX(index1)+cs2.getX(index2))*0.5;
        double new_y = (cs1.getY(index1)+cs2.getY(index2))*0.5;

        cs1.setOrdinate(index1,0,new_x);
        cs1.setOrdinate(index1,1,new_y);

        cs2.setOrdinate(index2,0,new_x);
        cs2.setOrdinate(index2,1,new_y);

        ls1.geometryChanged();
        ls2.geometryChanged();
    }

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
                Iterator<SlopeMark> it = slopeMarks.iterator();
                // Find slope within specified distance
                while (it.hasNext()) {
                    SlopeMark s = it.next();
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
                if (ls.getLength() > 0.01 && ls.getNumPoints() >= 2) {
                    ret.add(new Isoline(obj.getType(), slope_side, ls.getCoordinateSequence(), gf));
                } else {
                    System.out.println("Found invalid line string");
                }
            }
        }

        return ret;
    }

    public List<TOcadObject> getObjectsByID(int symbol_id) {
        List<TOcadObject> ret = new ArrayList<TOcadObject>();
        for (TOcadObject obj : objects) {
            if (obj.Sym == symbol_id) {
                ret.add(obj);
            }
        }
        return ret;
    }

    public List<TOcadObject> getObjectsByIDs(List<Integer> symbol_ids) {
        List<TOcadObject> ret = new ArrayList<TOcadObject>();
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


}
