package Deserialization;

import Isolines.IIsoline;
import Isolines.Isoline;
import Deserialization.Binary.*;
import Deserialization.Interpolation.CurveString;
import Deserialization.Interpolation.SlopeMark;
import Utils.Constants;
import Utils.GeomUtils;
import com.vividsolutions.jts.geom.*;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Artem on 21.07.2016.
 */
public class OcadDeserialization {

    public OcadDeserialization() {

    }

    public ArrayList<TOcadObject> objects;

    public void DeserializeMap( String path ) throws Exception {

        SeekableByteChannel ch = Files.newByteChannel(Paths.get(path)); // Defaults to read-only
        ch.position(0);

        OcadHeader header = new OcadHeader();
        header.Deserialize(ch,0);
        if ( header.OCADMark != 3245 ) throw new Exception("invalid format");
        if ( header.Version != 11 ) throw new Exception("invalid format");

        int previb = 0;
        int nextib = header.ObjectIndexBlock;
        TObjectIndexBlock ib = new TObjectIndexBlock();

        ArrayList<TOcadObject> lines = new ArrayList<>();
        objects = new ArrayList<>(2048);

        int hash = 0;
        int count = 0;

        while(nextib != 0) {

            ib.Deserialize(ch,nextib);

            previb = nextib;
            nextib = ib.NextObjectIndexBlock;

            for (int i = 0; i != 256; ++i) {
                TObjectIndex oi = ib.Table[i];
                if (!( oi.ObjType >= 0 && oi.ObjType <= 7 )) throw new Exception("invalid format");
                if (oi.ObjType == 2 || oi.ObjType == 1) {
                    //hash += oi.Pos;
                    //count += 1;
                    //ifile.seekg(oi.Pos);
                    //int len = oi.Len;
                    //lines.emplace_back( ifile );
                    TOcadObject obj = new TOcadObject();
                    obj.Deserialize(ch,oi.Pos);
                    objects.add(obj);
                }
            };
        };
        return;
    }

    public ArrayList<LineString> toLines(double interpolation_step, GeometryFactory gf) throws Exception {
        if (interpolation_step <= 0) throw new Exception("Invalid interpolation step");

        ArrayList<LineString> ret = new ArrayList<>();
        for (TOcadObject obj : objects) {
            ret.add(CurveString.fromTDPoly(obj.Poly).interpolate(interpolation_step,gf));
        }
        return ret;
    }


    public ArrayList<SlopeMark> slopeMarks;

    public ArrayList<IIsoline> toIsolines(double interpolation_step,GeometryFactory gf) throws Exception {
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
                LineString ls = CurveString.fromTDPoly(obj.Poly).interpolate(interpolation_step, gf);
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

}
