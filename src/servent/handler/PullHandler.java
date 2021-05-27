package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.PullMessage;
import servent.message.Message;
import servent.message.MessageType;

public class PullHandler extends TrackedMessageHandler {

//	private Message clientMessage;
	
//	public PullHandler(Message clientMessage) {
//		this.clientMessage = clientMessage;
//	}
	
	@Override
	public void run() {
		if (message.getMessageType() == MessageType.PULL) {
			PullMessage pullMessage = (PullMessage) message;

			AppConfig.chordState.pullFileForSomeoneElse(pullMessage);
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}