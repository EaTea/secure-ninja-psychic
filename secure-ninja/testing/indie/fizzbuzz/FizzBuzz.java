package indie.fizzbuzz;

import fb.fizz.Fizz;
import goo.buzz.Buzz;

public class FizzBuzz {

    public static void main(String[] args) {
        for (String s : args) {
            int k = Integer.parseInt(s);
            if (Fizz.fizz(k) && Buzz.buzz(k)) System.out.println("Fizzbuzz!");
            else if (Fizz.fizz(k)) System.out.println("Fizz!");
            else if (Buzz.buzz(k)) System.out.println("Buzz!");
        }
    }
}
