package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;

public class RemoveHandler implements MessageHandler {
    private Message clientMessage;

    public RemoveHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.REMOVE) {
            AppConfig.chordState.remove(clientMessage.getMessageText());
        } else {
            Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
        }

    }
}
