package indie.fizzbuzz;

public class FizzBuzz {

    public static void main(String[] args) {
        System.out.println("Hello customer 24601. I eat password. Feed me password...NOW!");
        java.io.Console c = System.console();
        if (!FizzBuzzAuth.checkPassword(new String(c.readPassword()))) {
            throw new IllegalArgumentException("Your password tastes disgusting. GRRRRRR.");
        }
        
        System.out.println("Yummy! Let the FizzleBuzzle-ing begin!");
        
        for (String s : args) {
            int k = Integer.parseInt(s);
            System.out.println(BuzzFizz.whatBuzz(k));
        }
    }
}
