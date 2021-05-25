package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;

public class SorryHandler implements MessageHandler {

	private Message clientMessage;
	
	public SorryHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SORRY) {
			Logger.timestampedStandardPrint("Couldn't enter Chord system because of collision. Change my listener port, please.");
			System.exit(0);
		} else {
			Logger.timestampedErrorPrint("Sorry handler got a message that is not SORRY");
		}

	}

}
