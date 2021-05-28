package servent.handler.data;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.SillyGitStorageFile;
import app.git.commit.CommitResult;
import app.merge.UnespectedPushResponseException;
import servent.handler.ResponseMessageHandler;
import servent.message.data.CommitResponseMessage;
import servent.message.MessageType;

import java.util.List;
import java.util.stream.Collectors;

public class CommitResponseHandler extends ResponseMessageHandler {

	private boolean isConflictResolution;

	public CommitResponseHandler(boolean isConflictResolution) {
		this.isConflictResolution = isConflictResolution;
	}
	
	@Override
	public void run() {
		if (message.getMessageType() == MessageType.COMMIT_RESPONSE) {
			CommitResponseMessage responseMessage = (CommitResponseMessage) message;

			CommitResult commitResult = responseMessage.getCommitResult(); //TODO: ne vredi mora se spavati

			if (isConflictResolution) { //must be only one file
				try {
					if (!commitResult.getSuccesses().isEmpty()) {
						SillyGitStorageFile sgsf = commitResult.getSuccesses().get(0);
						AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
						AppConfig.mergeResolver.pushResponseReceived(true);
					} else if (!commitResult.getFailedPaths().isEmpty()) {
						Logger.timestampedStandardPrint("Failed push for: " + commitResult.getFailedPaths().get(0));
						AppConfig.mergeResolver.pushResponseReceived(false);
					} else if (!commitResult.getConflicts().isEmpty()) {
						Logger.timestampedStandardPrint("Failed push - there was a conflict - for: " + commitResult.getFailedPaths().get(0));
						AppConfig.mergeResolver.pushResponseReceived(false);
					}
				} catch (UnespectedPushResponseException e) {
					Logger.timestampedErrorPrint("Merge resolver didn't expect push response");
				}
			} else {
				Logger.timestampedStandardPrint("--- Remote Commit result received:");
				Logger.timestampedStandardPrint("--- Success:");
				for(SillyGitStorageFile sgsf: commitResult.getSuccesses()) {
					AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
					Logger.timestampedStandardPrint("" + sgsf.getPathInStorageDir());
				}
				Logger.timestampedStandardPrint("--- Failed with errors:");
				for(String path: commitResult.getFailedPaths()) {
					Logger.timestampedStandardPrint("" + path);
				}
				Logger.timestampedStandardPrint("--- Conflicts for resolution:");
				List<String> confclitPaths = commitResult.getConflicts().stream().map(SillyGitFile::getPathInWorkDir).collect(Collectors.toList());
				for(String confclitPath: confclitPaths) {
					Logger.timestampedStandardPrint("" + confclitPath);
				}
				AppConfig.mergeResolver.addConflictsToResolve(confclitPaths);
			}

//			if (commitResult == null) {
//				if (isConflictResolution) {
//					try {
//						AppConfig.mergeResolver.pushResponseReceived(false);
//					} catch (UnespectedPushResponseException e) {
//						Logger.timestampedErrorPrint("Merge resolver didn't expect push response");
//					}
//				} else {
//					AppConfig.mergeResolver.addConflictToResolve(responseMessage.getMessageText());
//					Logger.timestampedErrorPrint("Commit conflict detected for file - " + message.getMessageText());
//				}
//			} else {
//				AppConfig.workDirectory.addFile(commitResult.getPathInStorageDir(), commitResult.getContent(), commitResult.getVersionHash());
//				if (isConflictResolution) {
//					try {
//						AppConfig.mergeResolver.pushResponseReceived(true);
//					} catch (UnespectedPushResponseException e) {
//						Logger.timestampedErrorPrint("Merge resolver didn't expect push response");
//					}
//				}
//			}
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}