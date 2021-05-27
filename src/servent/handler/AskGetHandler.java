package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.PullMessage;
import servent.message.Message;
import servent.message.MessageType;

public class AskGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public AskGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.PULL) {
			PullMessage message = (PullMessage) clientMessage;

			AppConfig.chordState.pullFileForSomeoneElse(message);
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}