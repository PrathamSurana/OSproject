import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        System.out.flush();

        // Read user input
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        
        // Print the invalid command message
        System.out.println(input + ": command not found");
    }
}
