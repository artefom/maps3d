package ru.ogpscenter.maps3d.algorithm.healing;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import ru.ogpscenter.maps3d.algorithm.repair.AttributedIsoline;
import ru.ogpscenter.maps3d.algorithm.repair.Connection;
import ru.ogpscenter.maps3d.algorithm.repair.LineConnector;
import ru.ogpscenter.maps3d.algorithm.repair.LineEnd;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.Isoline;
import ru.ogpscenter.maps3d.isolines.SlopeSide;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;
import ru.ogpscenter.maps3d.utils.Constants;
import ru.ogpscenter.maps3d.utils.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Created by Artem on 22.08.2016.
 */
public class Healer {

    private static void weldEnds(LineString ls1, LineString ls2, int le1, int le2) {
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

    public static void heal(Collection<IIsoline> in, GeometryFactory gf, BiConsumer<Integer, Integer> progressUpdate) {

        // todo(MS): update progress

        ArrayList<IIsoline> ret = new ArrayList<>();
        in.forEach(ret::add);

        int not_null_count = 0;
        int welded_count = 0;

        CommandLineUtils.reportProgressBegin("Welding points");
        // Connect ends...
        int ret_iter1 = 0;
        while (ret_iter1 < ret.size()) {
            CommandLineUtils.reportProgress(ret_iter1,ret.size());
            IIsoline iso1 = ret.get(ret_iter1);
            boolean connected = false;

            int ret_iter2 = ret_iter1;
            while (ret_iter2 < ret.size()) {
                IIsoline iso2 = ret.get(ret_iter2);

                // Create loop
                if (iso1 == iso2) {
                    LineString ls = iso1.getLineString();
                    if (ls.getCoordinateN(0).distance(ls.getCoordinateN(ls.getNumPoints()-1)) < Constants.CONNECTIONS_WELD_DIST) {
                        CoordinateSequence cs = ls.getCoordinateSequence();
                        if (cs.size() <= 3) {
                            connected = true;
                            break;
                        }
                        double new_x = (cs.getX(cs.size() - 1) + cs.getX(0)) * 0.5;
                        double new_y = (cs.getY(cs.size() - 1) + cs.getY(0)) * 0.5;
                        cs.setOrdinate(0, 0, new_x);
                        cs.setOrdinate(cs.size() - 1, 0, new_x);
                        cs.setOrdinate(0, 1, new_y);
                        cs.setOrdinate(cs.size() - 1, 1, new_y);
                        ls.geometryChanged();
                    }
                } else {
                    double min_d = Math.min(
                            Math.min(iso1.getLineString().getCoordinateN(0).distance(iso2.getLineString().getCoordinateN(0)),
                                    iso1.getLineString().getCoordinateN(0).distance(iso2.getLineString().getCoordinateN(iso2.getLineString().getNumPoints() - 1))),

                            Math.min(iso1.getLineString().getCoordinateN(iso1.getLineString().getNumPoints() - 1).distance(iso2.getLineString().getCoordinateN(0)),
                                    iso1.getLineString().getCoordinateN(iso1.getLineString().getNumPoints() - 1).distance(iso2.getLineString().getCoordinateN(iso2.getLineString().getNumPoints() - 1)))
                    );

                    if (min_d < Constants.CONNECTIONS_WELD_DIST) {

                        for (int i = 0; i != 2; ++i) {
                            for (int j = 0; j != 2; ++j) {
                                AttributedIsoline iso1_atr = new AttributedIsoline(iso1);
                                AttributedIsoline iso2_atr = new AttributedIsoline(iso2);
                                LineEnd le1 = LineEnd.fromIsoline(iso1_atr, i == 0 ? 1 : -1);
                                LineEnd le2 = LineEnd.fromIsoline(iso2_atr, j == 0 ? 1 : -1);

                                if (le1 != null && le2 != null) {
                                    not_null_count += 1;
                                    if (le1.line.p1.distance(le2.line.p1) < Constants.CONNECTIONS_WELD_DIST) {

                                        if (iso1.getType() == iso2.getType()) {
                                            welded_count += 1;
                                            Connection con = Connection.fromLineEnds(le1, le2);
                                            Pair<LineString, SlopeSide> new_line = LineConnector.connect(con, gf, true);
                                            if (new_line != null) ret.add(new Isoline(iso1.getType(), new_line.v2, new_line.v1.getCoordinateSequence(), gf));
                                            connected = true;
                                            break;
                                        } else {
                                            weldEnds(iso1.getLineString(),iso2.getLineString(),i == 0 ? 1 : -1, j == 0 ? 1 : -1);
                                        }
                                    }
                                    ;
                                }
                                ;
                            }
                            if (connected) break;
                        }
                    }
                }

                if (connected) {
                    ret.remove(ret_iter2);
                    if (ret_iter1 > ret_iter2) ret_iter1 -= 1;
                    break;
                } else {
                    ret_iter2 += 1;
                }
            }

            if (connected) {
                ret.remove(ret_iter1);
            } else {
                ret_iter1 += 1;
            }

        }
        CommandLineUtils.reportProgressEnd();

        int intersection_removed_count = 0;
        CommandLineUtils.reportProgressBegin("Removing intersections");
        ret_iter1 = 0;

        ArrayList<IIsoline> cutted_isolines = new ArrayList<>();
        while (ret_iter1 < ret.size()) {
            CommandLineUtils.reportProgress(ret_iter1, ret.size());
            IIsoline iso1 = ret.get(ret_iter1);
            LineString ls1 = iso1.getLineString();
            boolean deleted1 = false;

            int ret_iter2 = ret_iter1+1;
            while (ret_iter2 < ret.size()) {
                IIsoline iso2 = ret.get(ret_iter2);
                LineString ls2 = iso2.getLineString();

                if (ls1.intersects(ls2)) {

                    Geometry cutted1 = ls1.difference(ls2);
                    Geometry cutted2 = ls2.difference(ls1);

                    ArrayList<LineString> residue1;
                    {
                        if (cutted1.getNumGeometries() == 1) {
                            residue1 = new ArrayList<>();
                            residue1.add((LineString)cutted1.getGeometryN(0));
                        }
                        ArrayList<LineString> set1 = new ArrayList<>();
                        ArrayList<LineString> set2 = new ArrayList<>();

                        for (int i = 0; i < cutted1.getNumGeometries(); i+=2)
                            set1.add((LineString)cutted1.getGeometryN(i));

                        for (int i = 1; i < cutted1.getNumGeometries(); i+=2)
                            set2.add((LineString)cutted1.getGeometryN(i));

                        double set1_length = 0;
                        double set2_length = 0;

                        for (LineString ls : set1) set1_length += ls.getLength();
                        for (LineString ls : set2) set2_length += ls.getLength();

                        residue1 = set1_length > set2_length ? set1 : set2;
                    }

                    ArrayList<LineString> residue2;
                    {
                        if (cutted2.getNumGeometries() == 1) {
                            residue2 = new ArrayList<LineString>();
                            residue2.add((LineString)cutted2.getGeometryN(0));
                        }
                        ArrayList<LineString> set1 = new ArrayList<>();
                        ArrayList<LineString> set2 = new ArrayList<>();

                        for (int i = 0; i < cutted2.getNumGeometries(); i+=2)
                            set1.add((LineString)cutted2.getGeometryN(i));

                        for (int i = 1; i < cutted2.getNumGeometries(); i+=2)
                            set2.add((LineString)cutted2.getGeometryN(i));

                        double set1_length = 0;
                        double set2_length = 0;

                        for (LineString ls : set1) set1_length += ls.getLength();
                        for (LineString ls : set2) set2_length += ls.getLength();

                        residue2 = set1_length > set2_length ? set1 : set2;
                    }

                    double residue1_length = 0;
                    double residue2_length = 0;

                    for (LineString ls: residue1) residue1_length+=ls.getLength();
                    for (LineString ls: residue2) residue2_length+=ls.getLength();

                    double residue1_percent_loss = 1-(residue1_length/ls1.getLength());
                    double residue2_percent_loss = 1-(residue2_length/ls2.getLength());

                    intersection_removed_count += 1;

                    if (residue1_percent_loss < residue2_percent_loss) {
                        //IIsoline iso_new = new Isoline(iso1.getType(),iso1.getSlopeSide(),ls1.getCoordinateSequence(),gf);
                        //cutted_isolines.add( iso_new );
                        deleted1 = true;

                        for (LineString ls : residue1) {

                            IIsoline iso_new = new Isoline(iso1.getType(),iso1.getSlopeSide(),ls.getCoordinateSequence(),gf);

                            if (iso_new.getLineString().getLength() > Constants.CONNECTIONS_WELD_DIST)
                                cutted_isolines.add( iso_new );
                        }

                        break;
                    } else {

                        if (ret_iter1 > ret_iter2) ret_iter1 -= 1;
                        ret.remove(ret_iter2);

                        for (LineString ls : residue2) {
                            IIsoline iso_new = new Isoline(iso2.getType(),iso2.getSlopeSide(),ls.getCoordinateSequence(),gf);

                            if (iso_new.getLineString().getLength() > Constants.CONNECTIONS_WELD_DIST)
                                cutted_isolines.add( iso_new );
                        }

                        continue;

                    }

                }

                ret_iter2+=1;

            }

            if (deleted1) {
                ret.remove(ret_iter1);
            } else {
                ret_iter1 += 1;
            }


            cutted_isolines.forEach(ret::add);
            cutted_isolines.clear();
        }
        CommandLineUtils.reportProgressEnd();

        in.clear();
        for (IIsoline iso : cutted_isolines) {
            ret.add(iso);
        }

        ret.forEach(in::add);

        System.out.println("Intersections removed: "+intersection_removed_count);
        System.out.println("Points welded: "+welded_count);
    }
}
