package servent.message;

import app.ServentInfo;

public abstract class ResponseMessage extends TrackedMessage {

    public ResponseMessage(MessageType type, ServentInfo sender, ServentInfo receiver, String messageText) {
        super(type, sender, receiver, messageText);
    }

    public void tieResponseTo(TrackedMessage tm) {
        copyContextFrom(tm);
    }
}
