package servent.handler;

import app.AppConfig;
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
				AppConfig.timestampedErrorPrint("Got add message with bad text: " + clientMessage.getMessageText());
			}
			
			
		} else {
			AppConfig.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
