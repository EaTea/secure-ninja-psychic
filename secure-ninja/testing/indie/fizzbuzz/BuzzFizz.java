package indie.fizzbuzz;

import fb.fizz.Fizz;
import goo.buzz.Buzz;

public class BuzzFizz {
    public static String whatBuzz(int k) {
        if (Fizz.fizz(k) && Buzz.buzz(k)) return "Fizzbuzz!";
        else if (Fizz.fizz(k)) return "Fizz!";
        else if (Buzz.buzz(k)) return "Buzz!";
        return "No Buzz!";
    }
}
