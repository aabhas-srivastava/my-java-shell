package com.shell;

import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main shell class with JLine 3 integration
 */
public class JavaShell {
    private Terminal terminal;
    private LineReader reader;
    private CommandExecutor executor;
    private CommandHistory history;
    private Path currentDirectory;
    private List<String> commandHistory;

    public JavaShell() throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        
        this.executor = new CommandExecutor();
        this.history = new CommandHistory();
        this.commandHistory = new ArrayList<>();
        this.currentDirectory = Paths.get(System.getProperty("user.dir"));
        
        // Setup line reader with history and completion
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .history(history.getHistory())
                .completer(new StringsCompleter("cd", "pwd", "echo", "exit", "ls", "cat", "help", "history"))
                .build();
    }

    public void run() {
        printWelcomeMessage();
        
        while (true) {
            try {
                // Read command line with prompt
                String prompt = currentDirectory.toString() + "> ";
                String line = reader.readLine(prompt);
                
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                
                // Add to command history
                commandHistory.add(line);
                
                // Execute command
                if (!executeCommand(line)) {
                    break; // Exit if command returns false
                }
                
            } catch (UserInterruptException e) {
                // Ctrl+C
                terminal.writer().println("^C");
            } catch (EndOfFileException e) {
                // Ctrl+D
                terminal.writer().println("\nExiting...");
                break;
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
            }
        }
        
        try {
            terminal.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    private boolean executeCommand(String line) {
        try {
            // Parse command
            ParsedCommand parsed = CommandParser.parse(line);
            
            // Update current directory after cd command
            if (parsed.getCommand().equals("cd")) {
                String newDir = executor.executeBuiltIn(parsed, currentDirectory);
                if (newDir != null) {
                    currentDirectory = Paths.get(newDir).normalize();
                }
                return true;
            }
            
            // Exit command
            if (parsed.getCommand().equals("exit")) {
                return false;
            }
            
            // History command - handled separately
            if (parsed.getCommand().equals("history")) {
                printHistory();
                return true;
            }
            
            // Execute command
            executor.execute(parsed, currentDirectory, terminal);
            return true;
            
        } catch (Exception e) {
            terminal.writer().println("Error executing command: " + e.getMessage());
            return true;
        }
    }

    private void printWelcomeMessage() {
        terminal.writer().println("Welcome to Java Shell!");
        terminal.writer().println("Type 'help' for available commands, 'exit' to quit.");
        terminal.writer().println();
    }

    private void printHistory() {
        if (commandHistory.isEmpty()) {
            terminal.writer().println("No commands in history.");
        } else {
            for (int i = 0; i < commandHistory.size(); i++) {
                terminal.writer().println((i + 1) + "  " + commandHistory.get(i));
            }
        }
        terminal.writer().flush();
    }

    public static void main(String[] args) {
        try {
            JavaShell shell = new JavaShell();
            shell.run();
        } catch (Exception e) {
            System.err.println("Failed to initialize shell: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
