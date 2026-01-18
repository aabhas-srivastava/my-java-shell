package com.shell;

import org.jline.terminal.Terminal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes commands (both built-in and external)
 */
public class CommandExecutor {
    
    /**
     * Execute a command (built-in or external)
     */
    public void execute(ParsedCommand cmd, Path currentDirectory, Terminal terminal) throws IOException {
        // Handle pipelines
        if (cmd.isPiped() && !cmd.getPipeline().isEmpty()) {
            executePipeline(cmd.getPipeline(), currentDirectory, terminal);
            return;
        }
        
        // Check if it's a built-in command
        if (isBuiltInCommand(cmd.getCommand())) {
            String output = executeBuiltIn(cmd, currentDirectory);
            if (output != null) {
                writeOutput(output, cmd.getRedirection(), terminal, currentDirectory);
            }
        } else {
            // Execute external command
            executeExternal(cmd, currentDirectory, terminal);
        }
    }
    
    /**
     * Execute built-in command and return output
     * Returns new directory path for cd command
     */
    public String executeBuiltIn(ParsedCommand cmd, Path currentDirectory) {
        String command = cmd.getCommand();
        
        switch (command) {
            case "cd":
                return executeCd(cmd, currentDirectory);
            case "pwd":
                return currentDirectory.toAbsolutePath().toString() + "\n";
            case "echo":
                return executeEcho(cmd);
            case "exit":
                return null; // Handled in main loop
            case "help":
                return getHelpText();
            case "history":
                return ""; // Handled separately in shell
            default:
                return "Unknown built-in command: " + command + "\n";
        }
    }
    
    /**
     * Execute cd command
     */
    private String executeCd(ParsedCommand cmd, Path currentDirectory) {
        List<String> args = cmd.getArgs();
        
        if (args.isEmpty()) {
            // cd with no arguments goes to home directory
            String home = System.getProperty("user.home");
            return home != null ? home : currentDirectory.toString();
        }
        
        String target = args.get(0);
        Path newPath;
        
        if (target.equals("~") || target.equals("$HOME")) {
            String home = System.getProperty("user.home");
            newPath = home != null ? Paths.get(home) : currentDirectory;
        } else if (target.equals("-")) {
            // cd - goes to previous directory (simplified: use user home)
            String home = System.getProperty("user.home");
            newPath = home != null ? Paths.get(home) : currentDirectory;
        } else {
            newPath = currentDirectory.resolve(target).normalize();
        }
        
        if (Files.exists(newPath) && Files.isDirectory(newPath)) {
            return newPath.toAbsolutePath().toString();
        } else {
            System.err.println("cd: " + target + ": No such file or directory");
            return currentDirectory.toString();
        }
    }
    
    /**
     * Execute echo command
     */
    private String executeEcho(ParsedCommand cmd) {
        List<String> args = cmd.getArgs();
        if (args.isEmpty()) {
            return "\n";
        }
        
        // Join all arguments with spaces
        String result = String.join(" ", args);
        return result + "\n";
    }
    
    /**
     * Get help text
     */
    private String getHelpText() {
        return """
                Built-in Commands:
                  cd [directory]     - Change directory (default: home directory)
                  pwd                - Print working directory
                  echo [args...]     - Print arguments
                  exit               - Exit shell
                  help               - Show this help message
                  history            - Show command history
                
                Redirection:
                  command > file     - Redirect stdout to file (overwrite)
                  command >> file    - Redirect stdout to file (append)
                  command < file     - Redirect stdin from file
                  command 2> file    - Redirect stderr to file
                
                Pipelines:
                  command1 | command2 - Pipe stdout of command1 to stdin of command2
                
                Features:
                  - Auto-completion (Tab key)
                  - Command history (Up/Down arrows)
                  - Quote handling (single and double quotes)
                  - Escape sequences
                """;
    }
    
    /**
     * Check if command is built-in
     */
    private boolean isBuiltInCommand(String command) {
        return command != null && (
                command.equals("cd") ||
                command.equals("pwd") ||
                command.equals("echo") ||
                command.equals("exit") ||
                command.equals("help") ||
                command.equals("history")
        );
    }
    
    /**
     * Execute external command
     */
    private void executeExternal(ParsedCommand cmd, Path currentDirectory, Terminal terminal) 
            throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> commandAndArgs = new java.util.ArrayList<>();
        commandAndArgs.add(cmd.getCommand());
        commandAndArgs.addAll(cmd.getArgs());
        
        processBuilder.command(commandAndArgs);
        processBuilder.directory(currentDirectory.toFile());
        
        // Handle redirections
        RedirectionInfo redir = cmd.getRedirection();
        
        // Setup stdout redirection
        if (redir.hasStdoutRedirection()) {
            Path outputFile = currentDirectory.resolve(redir.getStdoutFile()).normalize();
            if (redir.getStdoutMode() == RedirectionMode.APPEND) {
                processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile.toFile()));
            } else {
                processBuilder.redirectOutput(ProcessBuilder.Redirect.to(outputFile.toFile()));
            }
        } else {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        
        // Setup stdin redirection
        if (redir.hasStdinRedirection()) {
            Path inputFile = currentDirectory.resolve(redir.getStdinFile()).normalize();
            processBuilder.redirectInput(ProcessBuilder.Redirect.from(inputFile.toFile()));
        } else {
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        }
        
        // Setup stderr redirection
        if (redir.hasStderrRedirection()) {
            Path errorFile = currentDirectory.resolve(redir.getStderrFile()).normalize();
            if (redir.getStderrMode() == RedirectionMode.APPEND) {
                processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(errorFile.toFile()));
            } else {
                processBuilder.redirectError(ProcessBuilder.Redirect.to(errorFile.toFile()));
            }
        } else {
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            // If no redirection, output goes to terminal (already handled by INHERIT)
            // Exit code is silently ignored unless needed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted", e);
        } catch (IOException e) {
            System.err.println("Error executing command: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute pipeline of commands
     * Simplified version: handles external commands only for now
     */
    private void executePipeline(List<ParsedCommand> pipeline, Path currentDirectory, Terminal terminal) 
            throws IOException {
        if (pipeline.isEmpty()) {
            return;
        }
        
        List<Process> processes = new java.util.ArrayList<>();
        List<Thread> transferThreads = new java.util.ArrayList<>();
        
        try {
            // Start all processes in pipeline
            for (int i = 0; i < pipeline.size(); i++) {
                ParsedCommand cmd = pipeline.get(i);
                
                // For now, skip built-in commands in pipeline (handle separately)
                if (isBuiltInCommand(cmd.getCommand())) {
                    // Handle built-in command output as string and feed to next command
                    if (i == pipeline.size() - 1) {
                        // Last command - just execute and output
                        String output = executeBuiltIn(cmd, currentDirectory);
                        if (output != null) {
                            RedirectionInfo redir = cmd.getRedirection();
                            if (redir.hasStdoutRedirection()) {
                                Path outputFile = currentDirectory.resolve(redir.getStdoutFile()).normalize();
                                if (redir.getStdoutMode() == RedirectionMode.APPEND) {
                                    Files.writeString(outputFile, output, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } else {
                                    Files.writeString(outputFile, output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                                }
                            } else {
                                terminal.writer().print(output);
                                terminal.writer().flush();
                            }
                        }
                        return; // Built-in as last command - done
                    }
                    // Built-in in middle - would need special handling
                    // For now, convert echo to external process
                    if (!cmd.getCommand().equals("echo")) {
                        terminal.writer().println("Warning: built-in command '" + cmd.getCommand() + "' in pipeline may not work correctly");
                    }
                }
                
                List<String> commandAndArgs = new java.util.ArrayList<>();
                commandAndArgs.add(cmd.getCommand());
                commandAndArgs.addAll(cmd.getArgs());
                
                ProcessBuilder builder = new ProcessBuilder(commandAndArgs);
                builder.directory(currentDirectory.toFile());
                
                // First command in pipeline
                if (i == 0) {
                    if (cmd.getRedirection().hasStdinRedirection()) {
                        Path inputFile = currentDirectory.resolve(cmd.getRedirection().getStdinFile()).normalize();
                        builder.redirectInput(ProcessBuilder.Redirect.from(inputFile.toFile()));
                    } else {
                        builder.redirectInput(ProcessBuilder.Redirect.PIPE);
                    }
                } else {
                    builder.redirectInput(ProcessBuilder.Redirect.PIPE);
                }
                
                // Last command in pipeline
                if (i == pipeline.size() - 1) {
                    RedirectionInfo redir = cmd.getRedirection();
                    if (redir.hasStdoutRedirection()) {
                        Path outputFile = currentDirectory.resolve(redir.getStdoutFile()).normalize();
                        if (redir.getStdoutMode() == RedirectionMode.APPEND) {
                            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile.toFile()));
                        } else {
                            builder.redirectOutput(ProcessBuilder.Redirect.to(outputFile.toFile()));
                        }
                    } else {
                        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }
                    
                    if (redir.hasStderrRedirection()) {
                        Path errorFile = currentDirectory.resolve(redir.getStderrFile()).normalize();
                        if (redir.getStderrMode() == RedirectionMode.APPEND) {
                            builder.redirectError(ProcessBuilder.Redirect.appendTo(errorFile.toFile()));
                        } else {
                            builder.redirectError(ProcessBuilder.Redirect.to(errorFile.toFile()));
                        }
                    } else {
                        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }
                } else {
                    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                    builder.redirectError(ProcessBuilder.Redirect.INHERIT);
                }
                
                Process process = builder.start();
                processes.add(process);
                
                // Connect previous process output to current process input
                if (i > 0 && processes.size() >= 2) {
                    Process prevProcess = processes.get(processes.size() - 2);
                    Process currProcess = process;
                    
                    Thread transferThread = new Thread(() -> {
                        try (InputStream in = prevProcess.getInputStream();
                             OutputStream out = currProcess.getOutputStream()) {
                            in.transferTo(out);
                        } catch (IOException e) {
                            // Ignore closed streams
                        }
                    });
                    transferThread.start();
                    transferThreads.add(transferThread);
                }
            }
            
            // Wait for all transfer threads
            for (Thread thread : transferThreads) {
                thread.join();
            }
            
            // Wait for all processes
            for (Process process : processes) {
                if (process != null) {
                    process.waitFor();
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            for (Process process : processes) {
                if (process != null) {
                    process.destroy();
                }
            }
            throw new IOException("Pipeline interrupted", e);
        }
    }
    
    /**
     * Write output to terminal or file based on redirection
     */
    private void writeOutput(String output, RedirectionInfo redir, Terminal terminal, Path currentDirectory) 
            throws IOException {
        if (redir.hasStdoutRedirection()) {
            Path outputFile = currentDirectory.resolve(redir.getStdoutFile()).normalize();
            if (redir.getStdoutMode() == RedirectionMode.APPEND) {
                Files.writeString(outputFile, output, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(outputFile, output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } else {
            terminal.writer().print(output);
            terminal.writer().flush();
        }
    }
}
