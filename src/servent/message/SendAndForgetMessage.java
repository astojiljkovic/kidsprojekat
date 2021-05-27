package servent.message;

import app.ServentInfo;

public abstract class SendAndForgetMessage extends BasicMessage {
    public SendAndForgetMessage(MessageType type, ServentInfo sender, ServentInfo receiver, String messageText) {
        super(type, sender, receiver, messageText);
    }

    public SendAndForgetMessage(MessageType type, ServentInfo sender, ServentInfo receiver) {
        super(type, sender, receiver);
    }
}
