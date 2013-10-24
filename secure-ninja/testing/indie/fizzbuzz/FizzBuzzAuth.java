package indie.fizzbuzz;

import java.util.HashMap;
import java.util.Map;

class FizzBuzzAuth {

    private static final Map<String, String> LICENSE_MAP = new HashMap<String, String>();
    
    /* <----- PASSWORD BEGIN -----> */
    private static final String PASSWORD = "";
    /* <----- PASSWORD END -----> */
    
    protected static void init() {
        /* <----- LICENSE PUT HERE -----> */
        LICENSE_MAP.put("fb.fizz.Fizz", "" /* fb.fizz.Fizz license goes here */);
        LICENSE_MAP.put("goo.buzz.Buzz", "");
        /* <----- END LICENSE PUT HERE -----> */
    }
    
    protected static String getLicense(String libName) {
        return LICENSE_MAP.get(libName);
    }
    
    protected static boolean checkPassword(String s) {
        return s.equals(PASSWORD);
    }
    
}
