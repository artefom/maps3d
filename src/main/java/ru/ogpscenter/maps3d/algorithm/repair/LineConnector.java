package ru.ogpscenter.maps3d.algorithm.repair;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import ru.ogpscenter.maps3d.utils.ArrayIterator;
import ru.ogpscenter.maps3d.utils.ArrayReverseIterator;
import ru.ogpscenter.maps3d.utils.CoordUtils;
import ru.ogpscenter.maps3d.utils.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Artem on 20.07.2016.
 */
public class LineConnector {

    /**
     * Merge two ru.ogpscenter.maps3d.isolines into one
     * Coordinates of first line are guaranteed to be before coordinates of second line
     * (First N coordinates of resulting line are combination of first line's coordinates,
     * where N is number of first line's coordinates)
     * @param con Connection, describing wich ru.ogpscenter.maps3d.isolines and wich ends are to be connected
     * @return IIsoline created by merging two
     */
    public static Pair<LineString,Integer> connect (Connection con, GeometryFactory gf, boolean collapse) {

        if (!con.isValid()) return null;

        if (con.first().isoline == con.second().isoline) {
            try {
                LineString ret = gf.createLinearRing(getLoopedCoordinates(con.first().isoline));
                return new Pair<>(ret, con.first().isoline.getSlopeSide());
            } catch (Exception ex) {
                return null;
            }
        }

        int result_ss;
        try {
            result_ss = con.resultSlopeSide();
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected case encountered");
        }

        LineString first_ls = con.first().isoline.getLineString();
        LineString second_ls = con.second().isoline.getLineString();

        List<Coordinate> coordinates = new ArrayList<>(first_ls.getNumPoints() +
                second_ls.getNumPoints());


        Iterator<Coordinate> first_iter;
        Iterator<Coordinate> second_iter;

        // Handle connection sides correctly
        first_iter = con.first().end_index == 1 ?
                new ArrayReverseIterator<>(first_ls.getCoordinates()) :
                new ArrayIterator<>(first_ls.getCoordinates());

        second_iter = con.second().end_index == 1 ?
                new ArrayIterator<>(second_ls.getCoordinates()) :
                new ArrayReverseIterator<>(second_ls.getCoordinates());


        if (collapse) {
            Coordinate coord = null;
            while (first_iter.hasNext()) {
                coord = first_iter.next();
                if (first_iter.hasNext())
                    coordinates.add(coord);
            }
            if (coord != null)
                coordinates.add( CoordUtils.div(CoordUtils.add(coord,second_iter.next()),2)  );
        } else {
            while (first_iter.hasNext()) {
                coordinates.add(first_iter.next());
            }
        }

        while (second_iter.hasNext()) {
            coordinates.add(second_iter.next());
        }

        LineString ls = gf.createLineString( coordinates.toArray(new Coordinate[coordinates.size()]));
        return new Pair<>(ls,result_ss);
    }

    /**
     * Connect last point of {@link LineString} with first to be able to create {@link com.vividsolutions.jts.geom.LinearRing}
     * @param is
     * @return
     */
    private static CoordinateSequence getLoopedCoordinates(AttributedIsoline is) {
        Coordinate[] al = new Coordinate[is.getGeometry().getNumPoints()+1];
        for (int i = 0; i != is.getLineString().getNumPoints(); ++i) {
            al[i] = new Coordinate( is.getLineString().getCoordinateN(i) );
        }
        al[al.length-1] = new Coordinate(al[0]);
        return is.getGeometry().getFactory().getCoordinateSequenceFactory().create(al);
    }
}
