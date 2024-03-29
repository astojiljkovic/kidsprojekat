package servent.handler.data;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import servent.handler.MessageHandler;
import servent.message.data.AddMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;

public class AddHandler implements MessageHandler {

	private Message clientMessage;
	
	public AddHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.ADD) {
				AddMessage addMessage = (AddMessage) clientMessage;

				List<SillyGitFile> sillyGitFiles = addMessage.getSillyGitFiles();
				AppConfig.chordState.addFileForSomeoneElse(sillyGitFiles, addMessage);
		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
