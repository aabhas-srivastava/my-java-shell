import org.jline.reader.impl.history.DefaultHistory;

/**
 * Wrapper for JLine history
 */
public class CommandHistory {
    private final DefaultHistory history;

    public CommandHistory() {
        this.history = new DefaultHistory();
    }

    public DefaultHistory getHistory() {
        return history;
    }
}
