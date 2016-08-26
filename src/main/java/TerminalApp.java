import Utils.DebugUtils;
import Utils.OutputUtils;

import java.io.File;

/**
 * Created by fdl on 8/4/16.
 */
public class TerminalApp {
    private static MainController mc = new MainController();

    private static void applyArgs(String[] args){
        if (args.length == 0) {
            System.err.println("TerminalApp [-m] <ocdFileName>");
            System.exit(-1);
        }
        String fileName = args[args.length-1];
        OutputUtils.setName(fileName.endsWith(".ocd") ? fileName.substring(0, fileName.length() - 4) : fileName);
        DebugUtils.skipBuildingMap = (args.length == 2 && args[0].contains("-m"));
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        applyArgs(args);

        if (DebugUtils.skipBuildingMap) {
            System.err.println("!!SKIPPING MAP BUILDING!!");
            mc.buildIndex();
        } else {
            mc.openFile(new File(args[args.length-1]));
            mc.detectEdge();
            mc.connectLines();
            mc.buildGraph();
            mc.interpolate("sample");
            mc.buildIndex();
        }

        System.out.println("Total execution time since start is " + (System.currentTimeMillis()-startTime)/1000 + " seconds.");
    }
}
