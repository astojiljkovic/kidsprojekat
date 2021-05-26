package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.storage.FileAlreadyAddedStorageException;
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

				SillyGitFile sgf = new SillyGitFile(fileName, content);
				try {
					AppConfig.chordState.addFile(sgf);
				} catch (FileAlreadyAddedStorageException e) { //TODO: Da li treba dodati poruku za slucaj da se ne uspe dodavanje?
					Logger.timestampedErrorPrint("Cannot add file - File already exists: " + e);
				}
			} else {
				Logger.timestampedErrorPrint("Got add message with bad text: " + clientMessage.getMessageText());
			}
			
			
		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
