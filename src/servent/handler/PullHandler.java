package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.PullMessage;
import servent.message.Message;
import servent.message.MessageType;

public class PullHandler implements MessageHandler {

	private Message clientMessage;
	
	public PullHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.PULL) {
			PullMessage pullMessage = (PullMessage) clientMessage;

			AppConfig.chordState.pullFileForSomeoneElse(pullMessage);
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}