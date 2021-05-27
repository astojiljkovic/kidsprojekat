package servent.message;

import app.ServentInfo;
import app.SillyGitFile;

public class CommitMessage extends TrackedMessage {

    private final SillyGitFile sgf;
    private final boolean force;

    public CommitMessage(ServentInfo sender, ServentInfo receiver, SillyGitFile file, boolean force) {
        super(MessageType.COMMIT, sender, receiver, "");
        this.sgf = file;
        this.force = force;
    }

    @Override
    protected String additionalContentToPrint() {
        return sgf.getPathInWorkDir() + "|" + sgf.getContent() + "|" + sgf.getStorageHash().orElse("") + "|Force:" + force;
    }

    public SillyGitFile getSgf() {
        return sgf;
    }

    public boolean getIsForce() {
        return force;
    }

    @Override
    public CommitMessage newMessageFor(ServentInfo next) {
        CommitMessage am = new CommitMessage(getSender(), next, sgf, force);
        am.copyContextFrom(this);
        return am;
    }
}
