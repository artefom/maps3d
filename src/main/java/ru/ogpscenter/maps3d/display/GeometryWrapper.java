package ru.ogpscenter.maps3d.display;

import com.vividsolutions.jts.geom.Geometry;
import javafx.scene.paint.Color;

/**
 * Created by Artyom.Fomenko on 15.07.2016.
 */
public class GeometryWrapper {
    public Geometry geom;

    public Color color;
    public double width;

    public GeometryWrapper( Geometry geom ) {
        this.geom = geom;
        this.color = Color.BLACK;
        this.width = 0.2;
    }

    public GeometryWrapper( Geometry geom, Color col, double width) {
        this.geom = geom;
        this.color = col;
        this.width = width;
    }
}
