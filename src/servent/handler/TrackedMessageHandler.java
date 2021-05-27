package servent.handler;

import servent.message.Message;
import servent.message.TrackedMessage;

public abstract class TrackedMessageHandler implements MessageHandler {
    TrackedMessage message = null;

    public void setMessage(TrackedMessage message) {
        this.message = message;
    }
}
