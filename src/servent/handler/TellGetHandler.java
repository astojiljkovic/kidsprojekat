package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;

public class TellGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public TellGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TELL_GET) {
			String []parts = clientMessage.getMessageText().split("<=>");
			
			if (parts.length == 2) {
				String fileName = parts[0];
				String content = parts[1];

				if (content.equals("FILE_NE_POSTOJI")) {
					Logger.timestampedStandardPrint("No such file with name: " + fileName);
				} else {
					Logger.timestampedStandardPrint(clientMessage.getMessageText());
				}
			} else {
				Logger.timestampedErrorPrint("Got TELL_GET message with bad text: " + clientMessage.getMessageText());
			}
		} else {
			Logger.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
