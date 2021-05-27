package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.storage.FileAlreadyAddedStorageException;
import servent.message.AddMessage;
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
			try {
				AddMessage addMessage = (AddMessage) clientMessage;

				List<SillyGitFile> sillyGitFiles = addMessage.getSillyGitFiles();
				AppConfig.chordState.addFileForSomeoneElse(sillyGitFiles, addMessage);
			} catch (FileAlreadyAddedStorageException e) { //TODO: Da li treba dodati poruku za slucaj da se ne uspe dodavanje?
				Logger.timestampedErrorPrint("Cannot add file - File already exists: " + e);
			}
		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
