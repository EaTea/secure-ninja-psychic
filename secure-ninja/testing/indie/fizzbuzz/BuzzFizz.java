package indie.fizzbuzz;

import fb.fizz.Fizz;
import goo.buzz.Buzz;

public class BuzzFizz {
    public static String whatBuzz(int k) {
        boolean isFizz = Fizz.fizz(k, FizzBuzzAuth.getLicense("fb.fizz.Fizz"));
        boolean isBuzz = Buzz.buzz(k, FizzBuzzAuth.getLicense("goo.buzz.Buzz"));
        if (isFizz && isBuzz) return "Fizzbuzz!";
        else if (isFizz) return "Fizz!";
        else if (isBuzz) return "Buzz!";
        return "No Buzz!";
    }
}
