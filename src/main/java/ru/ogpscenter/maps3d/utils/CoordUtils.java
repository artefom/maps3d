package ru.ogpscenter.maps3d.utils;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Artyom.Fomenko on 18.07.2016.
 * Class for performing operations on coordinates
 */
public class CoordUtils {

    public static Coordinate sub(Coordinate c1, Coordinate c2) {
        return new Coordinate(c1.x-c2.x,c1.y-c2.y);
    }

    public static Coordinate add(Coordinate c1, Coordinate c2) {
        return new Coordinate(c1.x+c2.x,c1.y+c2.y);
    }

    public static Coordinate add(Coordinate c1, double val) { return new Coordinate(c1.x+val,c1.y+val); };
    public static Coordinate sub(Coordinate c1, double val) { return new Coordinate(c1.x-val,c1.y-val); };

    public static Coordinate mul(Coordinate c1, double val) { return new Coordinate(c1.x*val,c1.y*val); };
    public static Coordinate div(Coordinate c1, double val) { return new Coordinate(c1.x/val,c1.y/val); };

    public static Coordinate weightedAverage( Coordinate c1, Coordinate c2, double pos) {
        return add(mul(c1,(1-pos)),mul(c2,pos));
    }

    public static void map(ArrayList<Coordinate> coords, double min_x, double max_x, double min_y, double max_y) {
        double in_min_x = coords.stream().min((lhs, rhs) -> Double.compare(lhs.x, rhs.x)).get().x;
        double in_max_x = coords.stream().max((lhs, rhs) -> Double.compare(lhs.x, rhs.x)).get().x;
        double in_min_y = coords.stream().min((lhs, rhs) -> Double.compare(lhs.y, rhs.y)).get().y;
        double in_max_y = coords.stream().max((lhs, rhs) -> Double.compare(lhs.y, rhs.y)).get().y;
        coords.forEach((c)->{
            c.x = GeomUtils.map(c.x,in_min_x,in_max_x,min_x,max_x);
            c.y = GeomUtils.map(c.y,in_min_y,in_max_y,min_y,max_y);
        });
    }

    public static ArrayList<Coordinate> coordinatesFromString(String str) {
        Matcher m = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)").matcher(str);
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        while (m.find()) {
            double x = Double.parseDouble(m.group(0));
            m.find();
            double y = Double.parseDouble(m.group(0));
            coordinates.add(new Coordinate(x,y));
        }

        return coordinates;
    }

    public static double getMinX(Collection<Coordinate> coordinates) {
        Iterator<Coordinate> it = coordinates.iterator();
        Coordinate c = it.next();
        double ret = c.x;
        while (it.hasNext()) {
            ret = Math.min(ret,c.x);
        }
        return ret;
    }

    public static double getMaxX(Collection<Coordinate> coordinates) {
        Iterator<Coordinate> it = coordinates.iterator();
        Coordinate c = it.next();
        double ret = c.x;
        while (it.hasNext()) {
            ret = Math.max(ret,c.x);
        }
        return ret;
    }

    public static double getMinY(Collection<Coordinate> coordinates) {
        Iterator<Coordinate> it = coordinates.iterator();
        Coordinate c = it.next();
        double ret = c.y;
        while (it.hasNext()) {
            ret = Math.min(ret,c.y);
        }
        return ret;
    }

    public static double getMaxY(Collection<Coordinate> coordinates) {
        Iterator<Coordinate> it = coordinates.iterator();
        Coordinate c = it.next();
        double ret = c.y;
        while (it.hasNext()) {
            ret = Math.max(ret,c.y);
        }
        return ret;
    }
}
