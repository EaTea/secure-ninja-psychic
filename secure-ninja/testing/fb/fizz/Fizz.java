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
        return LICENSE_STRING.equals(authString);
    }
}
