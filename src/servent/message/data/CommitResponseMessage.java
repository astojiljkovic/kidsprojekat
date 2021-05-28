package servent.message.data;

import app.ServentInfo;
import app.git.commit.CommitResult;
import servent.message.MessageType;
import servent.message.ResponseMessage;

public class CommitResponseMessage extends ResponseMessage {
//    private final SillyGitStorageFile sgsf;
    private final CommitResult commitResult;

    public CommitResponseMessage(ServentInfo sender, ServentInfo receiver, CommitResult commitResult) {
        super(MessageType.COMMIT_RESPONSE, sender, receiver, "");
        this.commitResult = commitResult;
    }

    @Override
    protected String additionalContentToPrint() {
        if (commitResult == null) {
            return "";
        }

        return commitResult.toString();
    }

    public CommitResult getCommitResult() {
        return commitResult;
    }

    @Override
    public CommitResponseMessage newMessageFor(ServentInfo next) {
        CommitResponseMessage message = new CommitResponseMessage(getSender(), next, commitResult);
        message.copyContextFrom(this);
        return message;
    }
}
