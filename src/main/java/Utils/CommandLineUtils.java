package Utils;

/**
 * Created by fdl on 8/10/16.
 */
public class CommandLineUtils {
    private static void printReport(String message){
        StackTraceElement stackTraceElement = new Throwable().getStackTrace()[2];
        System.out.println("\u001B[32m" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + " " + message + "\u001B[0m");
    }

    public static void printWarning(String message) {
        StackTraceElement stackTraceElement = new Throwable().getStackTrace()[2];
        System.out.println("\u001B[33mWarning: " + message + " at " + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + "\u001B[0m");
    }

    public static void printError(String message) {
        StackTraceElement stackTraceElement = new Throwable().getStackTrace()[2];
        System.out.println("\u001B[31mError: " + message + " at " + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + "\u001B[0m");
    }

    public static void report(){
        printReport("finished successfully");
    }

    public static void report(String customMessage){
        printReport(customMessage);
    }


    static boolean progress_reporting = false;
    static int currentMarksCount = 0;
    static int totalMarks = 50;

    public static void reportProgressBegin(String progressName) {
        System.out.print(progressName);
        for (int i =0; i < totalMarks-progressName.length(); ++i) System.out.print("_");
        System.out.println();
        progress_reporting = true;
        currentMarksCount = 0;
    }

    public static void reportProgress(int current, int total) {
        if (!progress_reporting) return;
        int newMarksCount = current*totalMarks/total;
        while(currentMarksCount < newMarksCount) {
            currentMarksCount += 1;
            System.out.print(".");
        }
    }

    public static void reportProgress(double doneFraction) {
        if (!progress_reporting) return;
        int newMarksCount = (int)(doneFraction*totalMarks);
        while(currentMarksCount < newMarksCount) {
            currentMarksCount += 1;
            System.out.print(".");
        }
    }

    public static void reportProgressEnd() {
        reportProgress(1);
        progress_reporting = false;
        System.out.println();
    }
}
