package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;

public class AddHandler implements MessageHandler {

	private Message clientMessage;
	
	public AddHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.ADD) {
			String[] splitText = clientMessage.getMessageText().split("<=>");
			if (splitText.length == 2) {
				String fileName = splitText[0];
				String content = splitText[1];

				AppConfig.chordState.addFile(fileName, content);
			} else {
				Logger.timestampedErrorPrint("Got add message with bad text: " + clientMessage.getMessageText());
			}
			
			
		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
