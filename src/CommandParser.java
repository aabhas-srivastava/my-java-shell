import java.util.ArrayList;
import java.util.List;

/**
 * Manual command parser with quote handling
 */
public class CommandParser {
    
    /**
     * Parse a command line into a ParsedCommand object
     * Handles quotes, redirection, and pipes
     */
    public static ParsedCommand parse(String line) {
        ParsedCommand result = new ParsedCommand();
        
        if (line == null || line.trim().isEmpty()) {
            return result;
        }
        
        line = line.trim();
        
        // Check for pipes first
        if (line.contains("|")) {
            String[] pipeParts = splitPipes(line);
            result.setPiped(true);
            result.setPipeline(parsePipeline(pipeParts));
            return result;
        }
        
        // Parse redirections
        RedirectionInfo redirection = parseRedirections(line);
        result.setRedirection(redirection);
        
        // Remove redirection operators from line for tokenization
        String commandPart = removeRedirections(line);
        
        // Tokenize command
        List<String> tokens = tokenize(commandPart);
        
        if (tokens.isEmpty()) {
            return result;
        }
        
        result.setCommand(tokens.get(0));
        result.setArgs(tokens.subList(1, tokens.size()));
        
        return result;
    }
    
    /**
     * Tokenize command line handling quotes and escaping
     */
    private static List<String> tokens = new ArrayList<>();
    private static StringBuilder currentToken = new StringBuilder();
    private static boolean inSingleQuotes = false;
    private static boolean inDoubleQuotes = false;
    
    private static List<String> tokenize(String line) {
        tokens.clear();
        currentToken.setLength(0);
        inSingleQuotes = false;
        inDoubleQuotes = false;
        
        char[] chars = line.toCharArray();
        int i = 0;
        
        while (i < chars.length) {
            char c = chars[i];
            
            if (c == '\\' && i + 1 < chars.length) {
                // Escape character
                char next = chars[i + 1];
                if (inDoubleQuotes) {
                    // In double quotes, escape certain chars
                    if (next == '"' || next == '\\' || next == '$' || next == '`') {
                        currentToken.append(next);
                        i += 2;
                    } else {
                        currentToken.append(c).append(next);
                        i += 2;
                    }
                } else if (!inSingleQuotes) {
                    // Single backslash followed by space/tab/newline
                    currentToken.append(next);
                    i += 2;
                } else {
                    // In single quotes, backslash is literal
                    currentToken.append(c);
                    i++;
                }
            } else if (c == '\'' && !inDoubleQuotes) {
                // Single quote
                inSingleQuotes = !inSingleQuotes;
                i++;
            } else if (c == '"' && !inSingleQuotes) {
                // Double quote
                inDoubleQuotes = !inDoubleQuotes;
                i++;
            } else if ((c == ' ' || c == '\t') && !inSingleQuotes && !inDoubleQuotes) {
                // Whitespace - end of token
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                i++;
            } else {
                currentToken.append(c);
                i++;
            }
        }
        
        // Add last token
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        
        return new ArrayList<>(tokens);
    }
    
    /**
     * Parse redirection operators (>, >>, <, 2>)
     */
    private static RedirectionInfo parseRedirections(String line) {
        RedirectionInfo info = new RedirectionInfo();
        List<String> tokens = tokenizeWithRedirections(line);
        
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            if (token.equals(">") && i + 1 < tokens.size()) {
                info.setStdoutFile(tokens.get(i + 1));
                info.setStdoutMode(RedirectionMode.OVERWRITE);
                i++;
            } else if (token.equals(">>") && i + 1 < tokens.size()) {
                info.setStdoutFile(tokens.get(i + 1));
                info.setStdoutMode(RedirectionMode.APPEND);
                i++;
            } else if (token.equals("<") && i + 1 < tokens.size()) {
                info.setStdinFile(tokens.get(i + 1));
                i++;
            } else if (token.startsWith("2>")) {
                if (token.equals("2>") && i + 1 < tokens.size()) {
                    info.setStderrFile(tokens.get(i + 1));
                    info.setStderrMode(RedirectionMode.OVERWRITE);
                    i++;
                } else if (token.equals("2>>") && i + 1 < tokens.size()) {
                    info.setStderrFile(tokens.get(i + 1));
                    info.setStderrMode(RedirectionMode.APPEND);
                    i++;
                }
            }
        }
        
        return info;
    }
    
    /**
     * Tokenize including redirection operators as separate tokens
     */
    private static List<String> tokenizeWithRedirections(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        
        char[] chars = line.toCharArray();
        int i = 0;
        
        while (i < chars.length) {
            char c = chars[i];
            
            if ((c == '"' || c == '\'') && (i == 0 || chars[i-1] != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                }
                currentToken.append(c);
                i++;
            } else if (!inQuotes && (c == '>' || c == '<')) {
                // Check for >> or >>
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString().trim());
                    currentToken.setLength(0);
                }
                
                // Handle >, >>, 2>, 2>>
                if (i + 1 < chars.length) {
                    char next = chars[i + 1];
                    if (c == '>' && next == '>') {
                        tokens.add(">>");
                        i += 2;
                    } else if (c == '2' && next == '>') {
                        if (i + 2 < chars.length && chars[i + 2] == '>') {
                            tokens.add("2>>");
                            i += 3;
                        } else {
                            tokens.add("2>");
                            i += 2;
                        }
                    } else {
                        tokens.add(String.valueOf(c));
                        i++;
                    }
                } else {
                    tokens.add(String.valueOf(c));
                    i++;
                }
            } else {
                if (c == ' ' || c == '\t') {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    i++;
                } else {
                    currentToken.append(c);
                    i++;
                }
            }
        }
        
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        
        return tokens;
    }
    
    /**
     * Remove redirection operators from command line
     */
    private static String removeRedirections(String line) {
        StringBuilder result = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        char[] chars = line.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if ((c == '"' || c == '\'') && (i == 0 || chars[i-1] != '\\')) {
                inQuotes = !inQuotes;
                quoteChar = c;
                result.append(c);
            } else if (!inQuotes && (c == '>' || c == '<')) {
                // Skip redirection operators and their arguments
                if (i + 1 < chars.length && chars[i + 1] == '>') {
                    i++; // Skip second >
                    // Skip until next space (the filename)
                    while (i + 1 < chars.length && chars[i + 1] == ' ') i++;
                    while (i + 1 < chars.length && chars[i + 1] != ' ') i++;
                } else if (i > 0 && chars[i - 1] == '2') {
                    // Handle 2> or 2>>
                    if (i + 1 < chars.length && chars[i + 1] == '>') i++;
                    // Skip until next space
                    while (i + 1 < chars.length && chars[i + 1] == ' ') i++;
                    while (i + 1 < chars.length && chars[i + 1] != ' ') i++;
                } else {
                    // Skip < and filename
                    while (i + 1 < chars.length && chars[i + 1] == ' ') i++;
                    while (i + 1 < chars.length && chars[i + 1] != ' ') i++;
                }
            } else {
                result.append(c);
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Split line by pipes, preserving quoted content
     */
    private static String[] splitPipes(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if ((c == '"' || c == '\'') && (i == 0 || line.charAt(i-1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                }
                current.append(c);
            } else if (c == '|' && !inQuotes) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Parse pipeline commands
     */
    private static List<ParsedCommand> parsePipeline(String[] parts) {
        List<ParsedCommand> pipeline = new ArrayList<>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            ParsedCommand cmd = new ParsedCommand();
            
            // Parse redirections only for first/last commands
            RedirectionInfo redir = i == 0 || i == parts.length - 1 ? parseRedirections(part) : new RedirectionInfo();
            cmd.setRedirection(redir);
            
            String commandPart = i == 0 || i == parts.length - 1 ? removeRedirections(part) : part;
            List<String> tokens = tokenize(commandPart);
            
            if (!tokens.isEmpty()) {
                cmd.setCommand(tokens.get(0));
                cmd.setArgs(tokens.subList(1, tokens.size()));
            }
            
            pipeline.add(cmd);
        }
        
        return pipeline;
    }
}
