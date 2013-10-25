package fb.fizz;

public class Fizz {

    /* LICENSE PUT HERE */
    private static final String LICENSE_STRING = "";
        
    public static boolean fizz(int k, String authString) {
        if (!fb.fizz.Fizz.verify(authString)) {
            throw new IllegalArgumentException("YOU STUPID PERSON!");
        }
        return 0 == (k % 3);
    }

    private static boolean verify(String authString) {
        // a method to determine if methods in Fizz can be used
        // we envisage every method in Fizz having a parameter for authString, and calling verify
        // to ensure that the provided authString is legitimate
        return LICENSE_STRING.equals(authString);
    }
}
