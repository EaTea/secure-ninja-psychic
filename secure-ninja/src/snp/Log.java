package snp;

/**
 * Our logging class.
 * Note: a more secure logging system would use a proper logging library instead of stdout and
 * stderr.
 * We provide a simple logging system for convenience, but also to note how this should be designed
 * in a larger, real-world application.
 * @author Edwin Tay(20529864) && Wan Ying Goh(20784663)
 * @version Oct 2013
 */
public class Log {

    /**
     * Error logging.
     * @param msg the error message
     */
    public static void error(String msg) {
        System.err.println("ERROR: " + msg);
    }
    
    /**
     * General logging.
     * @param msg the message
     */
    public static void log(String msg) {
        System.out.println("LOG: " + msg);
    }
    
    /**
     * Error logging.
     * @param format the format string
     * @param args the arguments
     */
    public static void error(String format, Object... args) {
        System.err.printf("ERROR: " + format, args);
    }
    
    /**
     * General logging.
     * @param format the format string
     * @param args the arguments
     */
    public static void log(String format, Object... args) {
        System.out.printf("LOG: " + format, args);
    }
    
    /**
     * Logging the end of the communication.
     */
    public static void logEnd() {
        log("<----- END COMMUNICATION ----->\n");
    }
}
