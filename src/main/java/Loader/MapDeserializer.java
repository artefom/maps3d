package Loader;

import Isolines.IIsoline;
import Isolines.Isoline;
import Loader.Binary.*;
import Loader.Interpolation.Curve;
import Loader.Interpolation.CurveString;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by Artem on 21.07.2016.
 */
public class MapDeserializer {

    public MapDeserializer() {

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

    public ArrayList<IIsoline> toIsolines(double interpolation_step,GeometryFactory gf) throws Exception {
        if (interpolation_step <= 0) throw new Exception("Invalid interpolation step");

        ArrayList<IIsoline> ret = new ArrayList<>();
        for (TOcadObject obj : objects) {
            if (obj.isLine()) {
                ret.add(new Isoline(obj.getType(),0,CurveString.fromTDPoly(obj.Poly).getCoordinateSequence(interpolation_step,gf),gf) );
            }
            //ret.add(CurveString.fromTDPoly(obj.Poly).interpolate(interpolation_step,gf));
        }
        return ret;
    }

}
