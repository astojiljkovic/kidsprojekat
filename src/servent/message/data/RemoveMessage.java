package servent.message.data;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class RemoveMessage extends TrackedMessage {

    private final String removePath;

    public RemoveMessage(ServentInfo sender, ServentInfo receiver, String removePath) {
        super(MessageType.REMOVE, sender, receiver, "");
        this.removePath = removePath;
    }

    public String getRemovePath() {
        return removePath;
    }

    @Override
    public RemoveMessage newMessageFor(ServentInfo next) {
        RemoveMessage am = new RemoveMessage(getSender(), next, removePath);
        am.copyContextFrom(this);
        return am;
    }
}
