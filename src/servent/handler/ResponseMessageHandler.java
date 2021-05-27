package servent.handler;

import servent.message.TrackedMessage;

public abstract class ResponseMessageHandler implements MessageHandler {
    TrackedMessage message = null;

    public void setMessage(TrackedMessage message) {
        this.message = message;
    }
    public void ResponseMessageHandler(TrackedMessage message) {
        this.message = message;
    }
}
