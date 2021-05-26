package servent.message;

import app.ServentInfo;
import app.SillyGitFile;

public class CommitMessage extends BasicMessage {

    private final SillyGitFile sgf;

    public CommitMessage(ServentInfo sender, ServentInfo receiver, SillyGitFile file) {
        super(MessageType.COMMIT, sender, receiver, "");
        this.sgf = file;
    }

    @Override
    protected String additionalContentToPrint() {
        return sgf.getPathInWorkDir() + "|" + sgf.getContent() + "|" + sgf.getStorageHash().orElse("");
    }

    public SillyGitFile getSgf() {
        return sgf;
    }

}
