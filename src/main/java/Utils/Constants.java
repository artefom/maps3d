package Utils;

import javafx.scene.paint.Color;

/**
 * Created by Artyom.Fomenko on 19.07.2016.
 */
public class Constants {


    //Connections
    public static final double CONNECTIONS_MIN_ANGLE_DEG=10;
    public static final double CONNECTIONS_MAX_ANGLE_DEG=100;
    public static final double CONNECTIONS_WELD_DIST = 0.5;
    public static final double CONNECTIONS_MAX_DIST = 12;

    //DRAWING
    public static final double DRAWING_LINE_WIDTH = 0.2;
    public static final double DRAWING_POINT_WIDTH = 0.8;
    public static final boolean DRAWING_INTERPOLATION = false;
    public static final double DRAWING_INTERPOLATION_STEP = 0.2;

        //COLORS
        public static final Color DRAWING_COLOR_ISOLINE_CLOSED = Color.GREEN;
        public static final Color DRAWING_COLOR_CONVEX_HULL = Color.RED;
        public static final Color DRAWING_COLOR_ISOLINE = Color.BLACK;
}
