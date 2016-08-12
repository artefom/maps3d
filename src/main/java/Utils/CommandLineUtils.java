package Utils;

/**
 * Created by fdl on 8/10/16.
 */
public class CommandLineUtils {
    private static void printReport(String message){
        StackTraceElement stackTraceElement = new Throwable().getStackTrace()[2];
        System.out.println("\u001B[32m" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + " " + message + "\u001B[0m");
    }

    public static void report(){
        printReport("finished successfully");
    }

    public static void report(String customMessage){
        printReport(customMessage);
    }
}
