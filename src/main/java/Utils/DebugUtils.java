package Utils;

/**
 * Used in debug. Should be removed on release
 */
public class DebugUtils {
    // Isoline id counter. (constructor of isoline increments this value by id and uses new value as isoline ID)
    public static int isoline_last_id = -1;
    public static boolean skipExternalSimplification = false;
    public static boolean skipBuildingMap = false;
}
