package servent.handler.data;

import app.AppConfig;
import app.Logger;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.data.RemoveMessage;

public class RemoveHandler implements MessageHandler {
    private Message clientMessage;

    public RemoveHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.REMOVE) {
            RemoveMessage rm = (RemoveMessage) clientMessage;
            AppConfig.chordState.removeFileFromSomeoneElse(rm);
//            AppConfig.chordState.removeFilesForUs(clientMessage.getMessageText());
        } else {
            Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
        }

    }
}
