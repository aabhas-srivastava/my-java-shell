/**
 * Contains redirection information for stdin, stdout, and stderr
 */
public class RedirectionInfo {
    private String stdinFile;
    private String stdoutFile;
    private String stderrFile;
    private RedirectionMode stdoutMode = RedirectionMode.OVERWRITE;
    private RedirectionMode stderrMode = RedirectionMode.OVERWRITE;

    public String getStdinFile() {
        return stdinFile;
    }

    public void setStdinFile(String stdinFile) {
        this.stdinFile = stdinFile;
    }

    public String getStdoutFile() {
        return stdoutFile;
    }

    public void setStdoutFile(String stdoutFile) {
        this.stdoutFile = stdoutFile;
    }

    public String getStderrFile() {
        return stderrFile;
    }

    public void setStderrFile(String stderrFile) {
        this.stderrFile = stderrFile;
    }

    public RedirectionMode getStdoutMode() {
        return stdoutMode;
    }

    public void setStdoutMode(RedirectionMode stdoutMode) {
        this.stdoutMode = stdoutMode;
    }

    public RedirectionMode getStderrMode() {
        return stderrMode;
    }

    public void setStderrMode(RedirectionMode stderrMode) {
        this.stderrMode = stderrMode;
    }

    public boolean hasStdoutRedirection() {
        return stdoutFile != null && !stdoutFile.isEmpty();
    }

    public boolean hasStdinRedirection() {
        return stdinFile != null && !stdinFile.isEmpty();
    }

    public boolean hasStderrRedirection() {
        return stderrFile != null && !stderrFile.isEmpty();
    }
}

enum RedirectionMode {
    OVERWRITE,
    APPEND
}
