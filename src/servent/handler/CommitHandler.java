package servent.handler;

import app.AppConfig;
import app.Logger;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;
import servent.message.Message;
import servent.message.MessageType;

public class CommitHandler implements MessageHandler {

	private Message clientMessage;

	public CommitHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.COMMIT) {
			String[] splitText = clientMessage.getMessageText().split("<=>");
			if (splitText.length == 2) {
				String fileName = splitText[0];
				String content = splitText[1];

				try {
					AppConfig.chordState.commitFileFromSomeoneElse(fileName, content, clientMessage.getSender());
				} catch (FileAlreadyAddedStorageException e) {
					Logger.timestampedErrorPrint("Cannot commit file - File already exists: " + e);
				} catch (FileDoesntExistStorageException e) { //TODO: Da li treba vratiti odgovor requesteru da ne postoji file vise / nikad nije ni addovan?
					Logger.timestampedErrorPrint("Cannot commit file - File doesn't exist: " + e);
				}
			} else {
				Logger.timestampedErrorPrint("Got commit message with bad text: " + clientMessage.getMessageText());
			}
			
			
		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
