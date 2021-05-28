package servent.handler;

import app.AppConfig;
import app.Logger;
import app.PullType;
import app.SillyGitStorageFile;
import app.git.pull.PullResult;
import app.merge.UnexpectedPullResponseException;
import servent.message.MessageType;
import servent.message.PullResponseMessage;

import java.util.List;

public class PullResponseHandler extends ResponseMessageHandler {

	private PullType pullType;

	public PullResponseHandler(PullType pullType) {
		super();
		this.pullType = pullType;
	}
	
	@Override
	public void run() {
		if (message.getMessageType() == MessageType.PULL_RESPONSE) {
			PullResponseMessage pullResponseMessage = (PullResponseMessage) message;
//			String requestedPath = pullResponseMessage.getMessageText(); //TODO: don't use - always empty. <- it's actually good.

//			List<SillyGitStorageFile> sgsf = pullResponseMessage.getSillyGitStorageFiles();
			PullResult pullResult = pullResponseMessage.getPullResult();

			try {
				switch (pullType) {
					case PULL:
						Logger.timestampedStandardPrint("Remote pull result received");
						Logger.timestampedStandardPrint(pullResult.toString());
						for (SillyGitStorageFile file: pullResult.getSuccesses()) {
							AppConfig.chordState.storeFileInWorkDir(file, false);
						}

//						if (sgsf != null) {
//							for (SillyGitStorageFile file: sgsf) {
//								AppConfig.chordState.storeFileInWorkDir(file, false);
//								Logger.timestampedStandardPrint("Successfully pulled file " + file.getPathInStorageDir());
//							}
//						} else {
//							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
//						}
						break;
					case CONFLICT_PULL:
						Logger.timestampedStandardPrint("Remote CONFLICT_PULL result received");
						Logger.timestampedStandardPrint(pullResult.toString());
						if (pullResult.getSuccesses().size() > 1) {
							throw new RuntimeException("CONFLICT PULL success result should always be at most 1");
						}
						for (SillyGitStorageFile file: pullResult.getSuccesses()) {
							AppConfig.chordState.storeFileInWorkDir(file, false);
//							Logger.timestampedStandardPrint("Successfully pulled file " + file.getPathInStorageDir());
						}
//						if (sgsf != null) {
//							for (SillyGitStorageFile file: sgsf) {
//								AppConfig.chordState.storeFileInWorkDir(file, false);
//								Logger.timestampedStandardPrint("Successfully pulled file " + file.getPathInStorageDir());
//							}
//						} else {
//							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
//						}
						boolean isSuccess = !pullResult.getSuccesses().isEmpty();
						AppConfig.mergeResolver.pullResponseReceived(isSuccess);
						break;
					case VIEW:
						Logger.timestampedStandardPrint("Remote VIEW result received");
						Logger.timestampedStandardPrint(pullResult.toString());
						if (pullResult.getSuccesses().size() > 1) {
							throw new RuntimeException("VIEW success result should always be at most 1");
						}
						for (SillyGitStorageFile file: pullResult.getSuccesses()) {
							AppConfig.chordState.storeFileInWorkDir(file, true);
//							Logger.timestampedStandardPrint("Successfully pulled file " + file.getPathInStorageDir());
						}

						boolean isSuccessView = !pullResult.getSuccesses().isEmpty();
						AppConfig.mergeResolver.viewResponseReceived(isSuccessView);
//						if (sgsf != null) {
//							for (SillyGitStorageFile file: sgsf) {
//								Logger.timestampedStandardPrint("Successfully fetched 'view' file for " + file.getPathInStorageDir());
//								AppConfig.chordState.storeFileInWorkDir(file, true);
//							}
//						} else {
//							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
//						}
//						AppConfig.mergeResolver.viewResponseReceived(sgsf != null);
						break;
				}
			} catch (UnexpectedPullResponseException e) {
				Logger.timestampedErrorPrint("Merge resolver didn't expect pull response");
			}
		} else {
			Logger.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
