package goo.buzz;

public class Buzz {
    
    /* LICENSE PUT HERE */
    private static final String LICENSE_STRING = "";

    public static boolean buzz(int k, String authString) {
        if (!Buzz.verify(authString)) {
            throw new IllegalArgumentException("I KNOW EVERYTHING");
        }
        return (k % 5) == 0;
    }

    private static boolean verify(String authString) {
        return LICENSE_STRING.equals(authString);
    }
}
