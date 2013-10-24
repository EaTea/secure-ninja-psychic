package goo.buzz;

public class Buzz {
    
    /* <----- MODIFY LICENSE -----> */
    private static final String LICENSE_STRING = "";
    /* <----- END MODIFY LICENSE -----> */

    public static boolean buzz(int k, String authString) {
        if (!Buzz.verify(authString)) {
            throw new IllegalArgumentException(authString);
        }
        return (k % 5) == 0;
    }

    private static boolean verify(String authString) {
        return LICENSE_STRING.equals(authString);
    }
}
