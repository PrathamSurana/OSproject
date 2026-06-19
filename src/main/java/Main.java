import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            String input = scanner.nextLine();
            
            if (input.equals("exit")) {
                break;
            } else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            } else if (input.startsWith("type ")) {
                String arg = input.substring(5);
                
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type")) {
                    System.out.println(arg + " is a shell builtin");
                } else {
                    String pathEnv = System.getenv("PATH");
                    String foundPath = null;
                    
                    if (pathEnv != null) {
                        String[] directories = pathEnv.split(File.pathSeparator);
                        for (String dir : directories) {
                            File file = new File(dir, arg);
                            if (file.exists() && file.canExecute()) {
                                foundPath = file.getAbsolutePath();
                                break;
                            }
                        }
                    }
                    
                    if (foundPath != null) {
                        System.out.println(arg + " is " + foundPath);
                    } else {
                        System.out.println(arg + ": not found");
                    }
                }
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}