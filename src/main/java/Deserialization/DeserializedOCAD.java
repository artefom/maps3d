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

        System.out.println("Meters per unit: "+this.metersPerUnit);

        Function<Pair<Integer,Integer>, Coordinate> coordinateConverter = ixy ->
                new Coordinate((double)ixy.v1/Constants.map_scale_fix*metersPerUnit,(double)ixy.v2/Constants.map_scale_fix*metersPerUnit);

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
                LineString ls = CurveString.fromOcadVertices(obj.vertices).interpolate(interpolation_step, gf);
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
//
//        int not_null_count = 0;
//        int welded_count = 0;
//        try {
//
//            CommandLineUtils.reportProgressBegin("Healing map");
//            // Connect ends...
//            int ret_iter1 = 0;
//            while (ret_iter1 < ret.size()) {
//                CommandLineUtils.reportProgress(ret_iter1,ret.size());
//                IIsoline iso1 = ret.get(ret_iter1);
//                boolean connected = false;
//
//                int ret_iter2 = ret_iter1;
//                while (ret_iter2 < ret.size()) {
//                    IIsoline iso2 = ret.get(ret_iter2);
//
//                    // Create loop
//                    if (iso1 == iso2) {
//                        LineString ls = iso1.getLineString();
//                        if (ls.getCoordinateN(0).distance(ls.getCoordinateN(ls.getNumPoints()-1)) < Constants.CONNECTIONS_WELD_DIST) {
//                            CoordinateSequence cs = ls.getCoordinateSequence();
//                            if (cs.size() <= 3) {
//                                connected = true;
//                                break;
//                            }
//                            double new_x = (cs.getX(cs.size() - 1) + cs.getX(0)) * 0.5;
//                            double new_y = (cs.getY(cs.size() - 1) + cs.getY(0)) * 0.5;
//                            cs.setOrdinate(0, 0, new_x);
//                            cs.setOrdinate(cs.size() - 1, 0, new_x);
//                            cs.setOrdinate(0, 1, new_y);
//                            cs.setOrdinate(cs.size() - 1, 1, new_y);
//                            ls.geometryChanged();
//                        }
//                    } else {
//                        double min_d = Math.min(
//                                Math.min(iso1.getLineString().getCoordinateN(0).distance(iso2.getLineString().getCoordinateN(0)),
//                                        iso1.getLineString().getCoordinateN(0).distance(iso2.getLineString().getCoordinateN(iso2.getLineString().getNumPoints() - 1))),
//
//                                Math.min(iso1.getLineString().getCoordinateN(iso1.getLineString().getNumPoints() - 1).distance(iso2.getLineString().getCoordinateN(0)),
//                                        iso1.getLineString().getCoordinateN(iso1.getLineString().getNumPoints() - 1).distance(iso2.getLineString().getCoordinateN(iso2.getLineString().getNumPoints() - 1)))
//                        );
//
//                        if (min_d < Constants.CONNECTIONS_WELD_DIST) {
//
//                            for (int i = 0; i != 2; ++i) {
//                                for (int j = 0; j != 2; ++j) {
//                                    Isoline_attributed iso1_atr = new Isoline_attributed(iso1);
//                                    Isoline_attributed iso2_atr = new Isoline_attributed(iso2);
//                                    LineEnd le1 = LineEnd.fromIsoline(iso1_atr, i == 0 ? 1 : -1);
//                                    LineEnd le2 = LineEnd.fromIsoline(iso2_atr, j == 0 ? 1 : -1);
//
//                                    if (le1 != null && le2 != null) {
//                                        not_null_count += 1;
//                                        if (le1.line.p1.distance(le2.line.p1) < Constants.CONNECTIONS_WELD_DIST) {
//
//                                            if (iso1.getType() == iso2.getType()) {
//                                                welded_count += 1;
//                                                Connection con = Connection.fromLineEnds(le1, le2);
//                                                Pair<LineString, Integer> new_line = LineConnector.connect(con, gf, true);
//                                                ret.add(new Isoline(iso1.getType(), new_line.v2, new_line.v1.getCoordinateSequence(), gf));
//                                                connected = true;
//                                                break;
//                                            } else {
//                                                weldEnds(iso1.getLineString(),iso2.getLineString(),i == 0 ? 1 : -1, j == 0 ? 1 : -1);
//                                            }
//                                        }
//                                        ;
//                                    }
//                                    ;
//                                }
//                                if (connected) break;
//                            }
//                        }
//                    }
//
//                    if (connected) {
//                        ret.remove(ret_iter2);
//                        if (ret_iter1 > ret_iter2) ret_iter1 -= 1;
//                        break;
//                    } else {
//                        ret_iter2 += 1;
//                    }
//                }
//
//                if (connected) {
//                    ret.remove(ret_iter1);
//                } else {
//                    ret_iter1 += 1;
//                }
//
//            }
//            CommandLineUtils.reportProgressEnd();
//
//            CommandLineUtils.reportProgressBegin("Removing intersections");
//            ret_iter1 = 0;
//
//            ArrayList<IIsoline> cutted_isolines = new ArrayList<>();
//            while (ret_iter1 < ret.size()) {
//                CommandLineUtils.reportProgress(ret_iter1, ret.size());
//                IIsoline iso1 = ret.get(ret_iter1);
//                LineString ls1 = iso1.getLineString();
//                boolean deleted1 = false;
//
//                int ret_iter2 = ret_iter1+1;
//                while (ret_iter2 < ret.size()) {
//                    IIsoline iso2 = ret.get(ret_iter2);
//                    LineString ls2 = iso2.getLineString();
//
//                    if (ls1.intersects(ls2)) {
//
//                        Geometry cutted1 = ls1.difference(ls2);
//                        Geometry cutted2 = ls2.difference(ls1);
//
//                        ArrayList<LineString> residue1;
//                        {
//                            if (cutted1.getNumGeometries() == 1) {
//                                residue1 = new ArrayList<LineString>();
//                                residue1.add((LineString)cutted1.getGeometryN(0));
//                            }
//                            ArrayList<LineString> set1 = new ArrayList<>();
//                            ArrayList<LineString> set2 = new ArrayList<>();
//
//                            for (int i = 0; i < cutted1.getNumGeometries(); i+=2)
//                                set1.add((LineString)cutted1.getGeometryN(i));
//
//                            for (int i = 1; i < cutted1.getNumGeometries(); i+=2)
//                                set2.add((LineString)cutted1.getGeometryN(i));
//
//                            double set1_length = 0;
//                            double set2_length = 0;
//
//                            for (LineString ls : set1) set1_length += ls.getLength();
//                            for (LineString ls : set2) set2_length += ls.getLength();
//
//                            residue1 = set1_length > set2_length ? set1 : set2;
//                        }
//
//                        ArrayList<LineString> residue2;
//                        {
//                            if (cutted2.getNumGeometries() == 1) {
//                                residue2 = new ArrayList<LineString>();
//                                residue2.add((LineString)cutted2.getGeometryN(0));
//                            }
//                            ArrayList<LineString> set1 = new ArrayList<>();
//                            ArrayList<LineString> set2 = new ArrayList<>();
//
//                            for (int i = 0; i < cutted2.getNumGeometries(); i+=2)
//                                set1.add((LineString)cutted2.getGeometryN(i));
//
//                            for (int i = 1; i < cutted2.getNumGeometries(); i+=2)
//                                set2.add((LineString)cutted2.getGeometryN(i));
//
//                            double set1_length = 0;
//                            double set2_length = 0;
//
//                            for (LineString ls : set1) set1_length += ls.getLength();
//                            for (LineString ls : set2) set2_length += ls.getLength();
//
//                            residue2 = set1_length > set2_length ? set1 : set2;
//                        }
//
//                        double residue1_length = 0;
//                        double residue2_length = 0;
//
//                        for (LineString ls: residue1) residue1_length+=ls.getLength();
//                        for (LineString ls: residue2) residue2_length+=ls.getLength();
//
//                        double residue1_percent_loss = 1-(residue1_length/ls1.getLength());
//                        double residue2_percent_loss = 1-(residue2_length/ls2.getLength());
//
//                        ArrayList<LineString> residue;
//
//                        if (residue1_percent_loss < residue2_percent_loss) {
//                            //IIsoline iso_new = new Isoline(iso1.getType(),iso1.getSlopeSide(),ls1.getCoordinateSequence(),gf);
//                            //cutted_isolines.add( iso_new );
//                            deleted1 = true;
//
//                            for (LineString ls : residue1) {
//                                IIsoline iso_new = new Isoline(iso1.getType(),iso1.getSlopeSide(),ls.getCoordinateSequence(),gf);
//                                cutted_isolines.add( iso_new );
//                            }
//
//                            break;
//                        } else {
//
//                            if (ret_iter1 > ret_iter2) ret_iter1 -= 1;
//                            ret.remove(ret_iter2);
//
//                            for (LineString ls : residue2) {
//                                IIsoline iso_new = new Isoline(iso2.getType(),iso2.getSlopeSide(),ls.getCoordinateSequence(),gf);
//                                cutted_isolines.add( iso_new );
//                            }
//
//                            break;
//
//                        }
//
//                    }
//
//                    ret_iter2+=1;
//
//                }
//
//                if (deleted1) {
//                    ret.remove(ret_iter1);
//                } else {
//                    ret_iter1 += 1;
//                }
//            }
//            CommandLineUtils.reportProgressEnd();
//
//            for (IIsoline iso : cutted_isolines) {
//                ret.add(iso);
//            }
//
//        } catch (Exception ex) {
//            System.out.println("FUCK: "+ex.getMessage());
//            //System.out.print(ex.getStackTrace());
//            for (StackTraceElement el : ex.getStackTrace()) {
//                System.out.println(el);
//            }
//        }
//
//        System.out.println("Welded count: "+welded_count);
//        System.out.println("not null count: " + not_null_count);
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
