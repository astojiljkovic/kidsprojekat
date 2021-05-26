package servent.message;

import app.ServentInfo;

public class CommitMessage extends BasicMessage {

    public CommitMessage(ServentInfo sender, ServentInfo receiver, String path, String content) {
        super(MessageType.COMMIT, sender, receiver, path + "<=>" + content);
    }
}
