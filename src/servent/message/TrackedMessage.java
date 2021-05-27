package servent.message;

import app.ServentInfo;

import java.io.Serializable;

public abstract class TrackedMessage extends BasicMessage {
    class Context implements Serializable {
        ServentInfo initiator;
        int initiatorMessageId;

        private Context(ServentInfo initiator, int initiatorMessageId) {
            this.initiator = initiator;
            this.initiatorMessageId = initiatorMessageId;
        }
    }

    private Context context;

    public TrackedMessage(MessageType type, ServentInfo sender, ServentInfo receiver, String messageText) {
        super(type, sender, receiver, messageText);
        context = new Context(sender, getMessageId());
    }

    public int getInitialId() {
        return context.initiatorMessageId;
    }

//    private TrackedMessage(MessageType type, ServentInfo sender, ServentInfo receiver, String messageText, Context context) {
//        super(type, sender, receiver, messageText);
//        this.context = context;
//    }

//    public TrackedMessage forward(ServentInfo next) {
//        TrackedMessage clone = clone();
//        return new TrackedMessage(getMessageType(), context.initiator, next, getMessageText(), context);
//    }
    public abstract TrackedMessage newMessageFor(ServentInfo next);

    public void copyContextFrom(TrackedMessage trackedMessage) {
        this.context = trackedMessage.context;
    }

    @Override
    public String toString() {
        return super.toString() + " C:[" + context.initiatorMessageId + "|" + context.initiator  + "]";
    }
}
