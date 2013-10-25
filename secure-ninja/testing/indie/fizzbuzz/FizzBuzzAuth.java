package indie.fizzbuzz;

import java.util.HashMap;
import java.util.Map;

class FizzBuzzAuth {

    // this file is a template file --- our in-memory compiler will replace some of the code
    // with licenses, and after compilation it will be protected since the source code is not
    // reverse engineerable
    private static final Map<String, String> LICENSE_MAP = new HashMap<String, String>();
    
    /* PASSWORD PUT HERE */
    private static final String PASSWORD = "";
    
    protected static void init() {
        /* <----- LICENSE PUT HERE -----> */
    }
    
    protected static String getLicense(String libName) {
        return LICENSE_MAP.get(libName);
    }
    
    protected static boolean checkPassword(String s) {
        return s.equals(PASSWORD);
    }
    
}
