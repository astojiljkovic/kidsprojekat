package servent.handler;

import app.AppConfig;
import app.Logger;
import app.PullType;
import app.SillyGitStorageFile;
import app.merge.UnexpectedPullResponseException;
import servent.message.MessageType;
import servent.message.PullResponseMessage;

import static app.PullType.CONFLICT_PULL;

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
			String requestedPath = pullResponseMessage.getMessageText();

			SillyGitStorageFile sgsf = pullResponseMessage.getSgsf();


//			if (pullType == CONFLICT_PULL) {
//				Logger.timestampedStandardPrint("pull type in if " + (pullType == CONFLICT_PULL));
//				AppConfig.mergeResolver.pullResponseReceived(sgsf != null);
//			}
			try {
				Logger.timestampedStandardPrint("pull type before switch " + pullType);
				switch (pullType) {
					case PULL:
						if (sgsf != null) {
							AppConfig.chordState.storeFileInWorkDir(sgsf, false);
							Logger.timestampedStandardPrint("Successfully pulled file " + sgsf);
						} else {
							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
						}
						break;
					case CONFLICT_PULL:
						if (sgsf != null) {
							AppConfig.chordState.storeFileInWorkDir(sgsf, false);
							Logger.timestampedStandardPrint("Successfully pulled file " + sgsf);
						} else {
							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
						}
						AppConfig.mergeResolver.pullResponseReceived(sgsf != null);
						Logger.timestampedStandardPrint("pull type before if " + pullType);
						if (pullType == CONFLICT_PULL) {
							Logger.timestampedStandardPrint("pull type in if " + (pullType == CONFLICT_PULL));
							AppConfig.mergeResolver.pullResponseReceived(sgsf != null);
						}
						break;
					case VIEW:
						if (sgsf != null) {
							Logger.timestampedStandardPrint("Successfully fetched 'view' file for " + sgsf.getPathInStorageDir());
						} else {
							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
						}
						AppConfig.mergeResolver.viewResponseReceived(sgsf != null);
						break;
				}
			} catch (UnexpectedPullResponseException e) {
				Logger.timestampedErrorPrint("Merge resolver didn't expect pull response");
			}
//			if (shouldSaveAsTemp) { //conflict resolution
//				if (sgsf == null) {
//					try {
//						AppConfig.mergeResolver.viewResponseReceived(false);
//					} catch (UnexpectedPullResponseException e) {
//						Logger.timestampedErrorPrint("Merge resolver didn't expect pull response");
//					}
//				} else {
//
//				}
//			}

//			if (sgsf == null) { //File not found in system
//				if (shouldSaveAsTemp) { //is conflict resolution (pull)
//					try {
//						AppConfig.mergeResolver.pullResponseReceived(false);
//					} catch (UnexpectedPullResponseException e) {
//						Logger.timestampedErrorPrint("Merge resolver didn't expect pull response");
//					}
//				} else {
//					Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
//				}
//			} else {
//				AppConfig.chordState.storeFileInWorkDir(sgsf, shouldSaveAsTemp);
//				Logger.timestampedStandardPrint("Successfully pulled file " + sgsf);
//				if (shouldSaveAsTemp) {
//					try {
//						AppConfig.mergeResolver.viewResponseReceived();
//					} catch (UnexpectedPullResponseException e) {
//						Logger.timestampedErrorPrint("Merge resolver didn't expect view response");
//					}
//				}
//			}
		} else {
			Logger.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
