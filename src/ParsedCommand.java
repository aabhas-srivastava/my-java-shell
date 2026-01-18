import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed command with arguments, redirection, and pipeline info
 */
public class ParsedCommand {
    private String command = "";
    private List<String> args = new ArrayList<>();
    private RedirectionInfo redirection = new RedirectionInfo();
    private boolean piped = false;
    private List<ParsedCommand> pipeline = new ArrayList<>();

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args != null ? args : new ArrayList<>();
    }

    public RedirectionInfo getRedirection() {
        return redirection;
    }

    public void setRedirection(RedirectionInfo redirection) {
        this.redirection = redirection != null ? redirection : new RedirectionInfo();
    }

    public boolean isPiped() {
        return piped;
    }

    public void setPiped(boolean piped) {
        this.piped = piped;
    }

    public List<ParsedCommand> getPipeline() {
        return pipeline;
    }

    public void setPipeline(List<ParsedCommand> pipeline) {
        this.pipeline = pipeline != null ? pipeline : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ParsedCommand{" +
                "command='" + command + '\'' +
                ", args=" + args +
                ", piped=" + piped +
                ", pipeline=" + pipeline.size() +
                '}';
    }
}
