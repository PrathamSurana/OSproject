import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            // Split input by spaces to separate command from its arguments
            String[] commands = input.split(" ");
            String command = commands[0];

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {
                System.out.println(input.substring(5));
            } else if (command.equals("type")) {
                String arg = commands[1];
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type")) {
                    System.out.println(arg + " is a shell builtin");
                } else {
                    String foundPath = getPath(arg);
                    if (foundPath != null) {
                        System.out.println(arg + " is " + foundPath);
                    } else {
                        System.out.println(arg + ": not found");
                    }
                }
            } else {
                // Look for an external executable in PATH
                String executablePath = getPath(command);
                if (executablePath != null) {
                    List<String> commandList = new ArrayList<>();
                    // Tip: Pass the raw command name (or full path depending on the tester requirement)
                    commandList.add(command); 
                    for (int i = 1; i < commands.length; i++) {
                        commandList.add(commands[i]);
                    }

                    ProcessBuilder pb = new ProcessBuilder(commandList);
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(input + ": command not found");
                }
            }
        }
    }

    private static String getPath(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] directories = pathEnv.split(File.pathSeparator);
            for (String dir : directories) {
                File file = new File(dir, command);
                if (file.exists() && file.canExecute()) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }
}