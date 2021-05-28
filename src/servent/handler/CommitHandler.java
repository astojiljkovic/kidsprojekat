package servent.handler;

import app.AppConfig;
import app.FileNotAddedFirstCommitException;
import app.Logger;
import app.git.commit.CommitResult;
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

			//Logs internally if errors occur
			//Will forward message to next node, or send response with CommitResoponse
			AppConfig.chordState.commitFileFromSomeoneElse(commitMessage);


		} else {
			Logger.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
