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

            List<String> parsedArgs = parseArguments(input);
            if (parsedArgs.isEmpty()) {
                continue;
            }

            String command = parsedArgs.get(0);

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parsedArgs.size(); i++) {
                    sb.append(parsedArgs.get(i));
                    if (i < parsedArgs.size() - 1) {
                        sb.append(" ");
                    }
                }
                System.out.println(sb.toString());
            } else if (command.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else if (command.equals("cd")) {
                String path = parsedArgs.size() > 1 ? parsedArgs.get(1) : "~";
                File dir;
                
                if (path.equals("~")) {
                    dir = new File(System.getenv("HOME"));
                } else if (path.startsWith("/")) {
                    dir = new File(path);
                } else {
                    dir = new File(System.getProperty("user.dir"), path);
                }

                if (dir.exists() && dir.isDirectory()) {
                    System.setProperty("user.dir", dir.getCanonicalPath());
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            } else if (command.equals("type")) {
                String arg = parsedArgs.get(1);
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
                    ProcessBuilder pb = new ProcessBuilder(parsedArgs);
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }

    private static List<String> parseArguments(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean hasContent = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && !inSingleQuotes) {
                if (inDoubleQuotes) {
                    // Inside double quotes, only escape specific characters
                    if (i + 1 < input.length()) {
                        char nextChar = input.charAt(i + 1);
                        if (nextChar == '"' || nextChar == '\\' || nextChar == '$' || nextChar == '`') {
                            currentArg.append(nextChar);
                            i++;
                        } else {
                            currentArg.append(c);
                        }
                    } else {
                        currentArg.append(c);
                    }
                } else {
                    // Outside quotes, always escape the next character
                    if (i + 1 < input.length()) {
                        currentArg.append(input.charAt(i + 1));
                        i++;
                    }
                }
                hasContent = true;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                hasContent = true;
            } else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                hasContent = true;
            } else if (c == ' ' && !inSingleQuotes && !inDoubleQuotes) {
                if (currentArg.length() > 0 || hasContent) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0);
                    hasContent = false;
                }
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0 || hasContent) {
            args.add(currentArg.toString());
        }

        return args;
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