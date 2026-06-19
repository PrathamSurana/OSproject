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

            String[] commands = input.split(" ");
            String command = commands[0];

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {
                System.out.println(input.substring(5));
            } else if (command.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else if (command.equals("cd")) {
                String path = commands[1];
                File dir;
                
                // If it's an absolute path, handle it directly. Otherwise, resolve relative to current user.dir
                if (path.startsWith("/")) {
                    dir = new File(path);
                } else {
                    dir = new File(System.getProperty("user.dir"), path);
                }

                // Canonical path automatically resolves "." and ".." components cleanly
                if (dir.exists() && dir.isDirectory()) {
                    System.setProperty("user.dir", dir.getCanonicalPath());
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            } else if (command.equals("type")) {
                String arg = commands[1];
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type") || arg.equals("pwd") || arg.equals("cd")) {
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
                String executablePath = getPath(command);
                if (executablePath != null) {
                    List<String> commandList = new ArrayList<>();
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