# Java Shell 🐚

A fun command-line shell built with Java 21 and JLine 3! Think of it as your friendly terminal companion that understands commands, runs programs, and helps you navigate your system. It's got all the essentials: built-in commands, redirection, piping, auto-completion, and command history - everything you need for a smooth command-line experience.

## What You Get

Here's what's packed inside:

- **Built-in Commands**: `cd`, `pwd`, `echo`, `exit`, `help`, `history` - your everyday tools
- **Run Anything**: Execute any system command you normally would
- **I/O Redirection**: Save output with `>`, `>>`, read files with `<`, catch errors with `2>`
- **Pipelines**: Chain commands together with `|` like a pro
- **Smart Quoting**: Handles single quotes, double quotes, and escape sequences properly
- **Tab Completion**: Just press Tab and let the magic happen
- **Command History**: Use those arrow keys to browse through what you've typed

## What You'll Need

Before we start, make sure you have:

- Java 21 or newer (check with `java -version`)
- Maven 3.6+ (for building)

That's it! Pretty simple, right?

## Getting Started

First, let's build the project:

```bash
mvn clean package
```

This creates a JAR file at `target/java-shell-1.0-SNAPSHOT.jar` that you can run anywhere.

Then fire it up:

```bash
java -jar target/java-shell-1.0-SNAPSHOT.jar
```

Or if you want to compile and run in one go:

```bash
mvn compile exec:java -Dexec.mainClass="JavaShell"
```

## Using the Shell

Once you're in, you can use built-in commands like `cd`, `pwd`, and `echo`, or run any external system command. Redirect output to files with `>`, `>>`, read from files with `<`, or capture errors with `2>`. Chain commands together with pipes (`|`) to build powerful command pipelines.

Press `Tab` to auto-complete commands, use the `Up/Down` arrows to browse your command history, and type `help` to see what's available. When you're done, just type `exit` to quit.

**Quick Tips:**
- `Tab` - Auto-complete commands as you type
- `Up/Down Arrows` - Scroll through your command history
- `Ctrl+C` - Stop whatever's running (emergency exit!)
- `Ctrl+D` - Exit the shell gracefully

## Behind the Scenes

If you're curious about how it works, here's the structure:

```
src/
├── JavaShell.java          # The main brain - handles the REPL loop
├── CommandParser.java      # Takes your text and figures out what you mean
├── CommandExecutor.java    # Actually runs commands (built-in or external)
├── ParsedCommand.java      # A neat wrapper for parsed commands
├── RedirectionInfo.java    # Keeps track of all the >, >>, < stuff
└── CommandHistory.java     # Manages your command history
```

**The Flow:**
1. You type something → `JavaShell` reads it
2. `CommandParser` figures out what you mean (handles quotes, pipes, redirections)
3. `CommandExecutor` runs it (either built-in or as an external process)
4. Output comes back to you → repeat!

It's a simple but powerful pipeline that makes everything work smoothly.

## What's Not There (Yet)

This shell does a lot, but there are a few things that aren't fully implemented yet:

- Built-in commands in pipelines work, but in a simplified way
- Variable expansion in double quotes is planned but not fully there
- Background processes (`&`) aren't supported yet
- Command substitution (backticks like `` `command` ``) isn't available

These might come in future versions, or you could add them yourself if you're feeling adventurous!

## About This Project

This started as a learning exercise - a fun way to understand how shells work under the hood. Building your own shell teaches you about parsing, process management, I/O redirection, and terminal interaction. It's been a blast to build, and i hope you enjoy using it too!

**Thank you!**