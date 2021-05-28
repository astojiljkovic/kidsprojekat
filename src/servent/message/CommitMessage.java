package servent.message;

import app.ServentInfo;
import app.SillyGitFile;

import java.util.List;
import java.util.stream.Collectors;

public class CommitMessage extends TrackedMessage {

    private final List<SillyGitFile> filesToCommit;
    private final boolean force;

    public CommitMessage(ServentInfo sender, ServentInfo receiver, List<SillyGitFile> filesToCommit, boolean force) {
        super(MessageType.COMMIT, sender, receiver, "");
        this.filesToCommit = filesToCommit;
        this.force = force;
    }

    @Override
    protected String additionalContentToPrint() {
        return filesToCommit.stream().map(sgf -> {
            return "<" + sgf.getPathInWorkDir() + "|" + sgf.getContent() + "|" + sgf.getStorageHash().orElse("") + ">";
        }).collect(Collectors.joining()) + "|Force:" + force;
    }

    public List<SillyGitFile> getFilesToCommit() {
        return filesToCommit;
    }

    public boolean getIsForce() {
        return force;
    }

    @Override
    public CommitMessage newMessageFor(ServentInfo next) {
        CommitMessage am = new CommitMessage(getSender(), next, filesToCommit, force);
        am.copyContextFrom(this);
        return am;
    }
}
