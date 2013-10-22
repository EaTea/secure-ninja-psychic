package indie.fizzbuzz;

public class FizzBuzz {

    public static void main(String[] args) {
        for (String s : args) {
            int k = Integer.parseInt(s);
            System.out.println(BuzzFizz.whatBuzz(k));
        }
    }
}
