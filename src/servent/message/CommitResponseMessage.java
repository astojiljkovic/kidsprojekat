package servent.message;

import app.ServentInfo;
import app.SillyGitStorageFile;

public class CommitResponseMessage extends ResponseMessage {
    private final SillyGitStorageFile sgsf;

    public CommitResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, SillyGitStorageFile sgsf) {
        super(MessageType.COMMIT_RESPONSE, sender, receiver, requestedPath);
        this.sgsf = sgsf;
    }

    @Override
    protected String additionalContentToPrint() {
        if (sgsf == null) {
            return "";
        }
        return sgsf.getPathInStorageDir() + "|" + sgsf.getContent() + "|" + sgsf.getVersionHash();
    }

    public SillyGitStorageFile getSgsf() {
        return sgsf;
    }

    @Override
    public CommitResponseMessage newMessageFor(ServentInfo next) {
        CommitResponseMessage message = new CommitResponseMessage(getSender(), next, getMessageText(), sgsf);
        message.copyContextFrom(this);
        return message;
    }
}
