package servent.message;

import app.ServentInfo;

public class RemoveMessage extends BasicMessage {

    public RemoveMessage(ServentInfo sender, ServentInfo receiver, String path) {
        super(MessageType.REMOVE, sender, receiver, path);
    }
}
