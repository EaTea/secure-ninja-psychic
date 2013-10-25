package snp;

public class Log {

    public static void error(String msg) {
        System.err.println("ERROR: " + msg);
    }
    
    public static void log(String msg) {
        System.out.println("LOG: " + msg);
    }
    
    public static void error(String format, Object... args) {
        System.err.printf("ERROR: " + format, args);
    }
    
    public static void log(String format, Object... args) {
        System.out.printf("LOG: " + format, args);
    }
    
    public static void logEnd() {
        log("<----- END COMMUNICATION ----->\n");
    }
}
