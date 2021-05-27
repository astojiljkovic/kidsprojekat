package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitStorageFile;
import app.merge.UnespectedPushResponseException;
import servent.message.CommitResponseMessage;
import servent.message.MessageType;

public class CommitResponseHandler extends ResponseMessageHandler {

	private boolean isConflictResolution;

	public CommitResponseHandler(boolean isConflictResolution) {
		this.isConflictResolution = isConflictResolution;
	}
	
	@Override
	public void run() {
		if (message.getMessageType() == MessageType.COMMIT_RESPONSE) {
			CommitResponseMessage responseMessage = (CommitResponseMessage) message;

			SillyGitStorageFile sgsf = responseMessage.getSgsf();
			if (sgsf == null) {
				if (isConflictResolution) {
					try {
						AppConfig.mergeResolver.pushResponseReceived(false);
					} catch (UnespectedPushResponseException e) {
						Logger.timestampedErrorPrint("Merge resolver didn't expect push response");
					}
				} else {
					AppConfig.mergeResolver.addConflictToResolve(responseMessage.getMessageText());
					Logger.timestampedErrorPrint("Commit conflict detected for file - " + message.getMessageText());
				}
			} else {
				AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
				if (isConflictResolution) {
					try {
						AppConfig.mergeResolver.pushResponseReceived(true);
					} catch (UnespectedPushResponseException e) {
						Logger.timestampedErrorPrint("Merge resolver didn't expect push response");
					}
				}
			}
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}