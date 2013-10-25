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
        // a method to determine if methods in Buzz can be used
        // we envisage every method in Buzz having a parameter for authString, and calling verify
        // to ensure that the provided authString is legitimate
        return LICENSE_STRING.equals(authString);
    }
}
