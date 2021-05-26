package servent.handler;

import app.AppConfig;
import app.FileNotAddedFirstCommitException;
import app.Logger;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;
import servent.message.AddMessage;
import servent.message.CommitMessage;
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
			CommitMessage commitMessage = (CommitMessage) clientMessage;

			try {
				AppConfig.chordState.commitFileFromSomeoneElse(commitMessage.getSgf(), clientMessage.getSender());
			} catch (FileAlreadyAddedStorageException e) {
				Logger.timestampedErrorPrint("Cannot commit file - File already exists: " + commitMessage.getSgf());
			} catch (FileDoesntExistStorageException e) { //TODO: Da li treba vratiti odgovor requesteru da ne postoji file vise / nikad nije ni addovan?
				Logger.timestampedErrorPrint("Cannot commit file - File doesn't exist: " + commitMessage.getSgf());
			} catch (FileNotAddedFirstCommitException e) { //TODO: Da li treba vratiti odgovor requesteru da ne postoji file vise / nikad nije ni addovan?
				Logger.timestampedErrorPrint("Cannot commit file - File has to be added first " + commitMessage.getSgf());
			} catch (CommitConflictStorageException e) { //TODO: Da li treba vratiti odgovor requesteru da ne postoji file vise / nikad nije ni addovan?
				Logger.timestampedErrorPrint("Cannot commit file - Exception encountered " + commitMessage.getSgf());
			}

		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
