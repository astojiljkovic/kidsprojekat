package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitStorageFile;
import servent.message.AddResponseMessage;
import servent.message.CommitResponseMessage;
import servent.message.Message;
import servent.message.MessageType;

public class CommitResponseHandler implements MessageHandler {

	private Message clientMessage;

	public CommitResponseHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.COMMIT_RESPONSE) {
			CommitResponseMessage responseMessage = (CommitResponseMessage) clientMessage;

			SillyGitStorageFile sgsf = responseMessage.getSgsf();
			if (sgsf == null) {
				Logger.timestampedErrorPrint("Commit conflict detected for file - " + clientMessage.getMessageText());

			} else {
				AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
			}
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}