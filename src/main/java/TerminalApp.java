import Algorithm.BuildingIndex.TerrainContainer;
import Algorithm.Interpolation.DistanceFieldInterpolation;
import Utils.DebugUtils;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by fdl on 8/4/16.
 */
public class TerminalApp {
    static MainController mc = new MainController();

    private static void applyParams(String s){
        if (s.contains("s")) DebugUtils.skipExternalSimplification = true;
        if (s.contains("b")) DebugUtils.skipBuildingMap = true;
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        assert args.length > 0 && args[args.length-1].endsWith(".ocd") : "no input OCAD file specified";
        if (args.length == 2) applyParams(args[0]);

        try {
            mc.openFile(new File(args[args.length-1]));
            System.out.println("Added " + mc.IsolineCount() + " isolines. Bbox: " + mc.ic.getEnvelope());
        } catch (FileNotFoundException ex) {
            System.err.println("File not found");
        } catch (IOException ex) {
            System.err.println("File reading error: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("File parsing error: " + ex.getMessage());
        }

        if (DebugUtils.skipBuildingMap) {
            System.err.println("SKIPPING WHOLE BUILDING");
        } else {
            mc.detectEdge();
            mc.connectLines();
            mc.buildGraph();
            mc.interpolate();
        }
        new TerrainContainer(new DistanceFieldInterpolation(mc.ic).getAllInterpolatingPoints());
        System.out.println("Total time since start is " + (System.currentTimeMillis()-startTime)/1000 + " seconds.");
    }
}
