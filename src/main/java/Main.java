import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        while (true) {
            System.setOut(originalOut);
            System.setErr(originalErr);
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

            // Extract redirection details if present
            String redirectFile = null;
            String redirectType = null; // "stdout" or "stderr"
            boolean append = false;
            int redirectIndex = -1;

            for (int i = 0; i < parsedArgs.size(); i++) {
                String arg = parsedArgs.get(i);
                if (arg.equals(">") || arg.equals("1>")) {
                    if (i + 1 < parsedArgs.size()) {
                        redirectFile = parsedArgs.get(i + 1);
                        redirectType = "stdout";
                        append = false;
                        redirectIndex = i;
                        break;
                    }
                } else if (arg.equals(">>") || arg.equals("1>>")) {
                    if (i + 1 < parsedArgs.size()) {
                        redirectFile = parsedArgs.get(i + 1);
                        redirectType = "stdout";
                        append = true;
                        redirectIndex = i;
                        break;
                    }
                } else if (arg.equals("2>")) {
                    if (i + 1 < parsedArgs.size()) {
                        redirectFile = parsedArgs.get(i + 1);
                        redirectType = "stderr";
                        append = false;
                        redirectIndex = i;
                        break;
                    }
                }
            }

            List<String> commandArgs = parsedArgs;
            if (redirectIndex != -1) {
                commandArgs = new ArrayList<>(parsedArgs.subList(0, redirectIndex));
            }

            if (commandArgs.isEmpty()) {
                continue;
            }

            String command = commandArgs.get(0);

            // Handle redirection stream setup
            File outFile = null;
            if (redirectFile != null) {
                outFile = new File(redirectFile);
                if (outFile.getParentFile() != null) {
                    outFile.getParentFile().mkdirs();
                }
                PrintStream fileOut = new PrintStream(new FileOutputStream(outFile, append));
                if (redirectType.equals("stdout")) {
                    System.setOut(fileOut);
                } else if (redirectType.equals("stderr")) {
                    System.setErr(fileOut);
                }
            }

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < commandArgs.size(); i++) {
                    sb.append(commandArgs.get(i));
                    if (i < commandArgs.size() - 1) {
                        sb.append(" ");
                    }
                }
                System.out.println(sb.toString());
            } else if (command.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else if (command.equals("cd")) {
                String path = commandArgs.size() > 1 ? commandArgs.get(1) : "~";
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
                    System.setOut(originalOut);
                    System.err.println("cd: " + path + ": No such file or directory");
                }
            } else if (command.equals("type")) {
                String arg = commandArgs.get(1);
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
                    ProcessBuilder pb = new ProcessBuilder(commandArgs);
                    if (outFile != null) {
                        if (redirectType.equals("stdout")) {
                            if (append) {
                                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
                            } else {
                                pb.redirectOutput(outFile);
                            }
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        } else if (redirectType.equals("stderr")) {
                            pb.redirectError(outFile);
                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        }
                    } else {
                        pb.inheritIO();
                    }
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.setOut(originalOut);
                    System.err.println(command + ": command not found");
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