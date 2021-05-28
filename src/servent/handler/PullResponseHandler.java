package servent.handler;

import app.AppConfig;
import app.Logger;
import app.PullType;
import app.SillyGitStorageFile;
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
			String requestedPath = pullResponseMessage.getMessageText(); //TODO: don't use - always empty

			List<SillyGitStorageFile> sgsf = pullResponseMessage.getSillyGitStorageFiles();

			try {
				switch (pullType) {
					case PULL:
						if (sgsf != null) {
							for (SillyGitStorageFile file: sgsf) {
								AppConfig.chordState.storeFileInWorkDir(file, false);
								Logger.timestampedStandardPrint("Successfully pulled file " + file.getPathInStorageDir());
							}
						} else {
							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
						}
						break;
					case CONFLICT_PULL:
						if (sgsf != null) {
							for (SillyGitStorageFile file: sgsf) {
								AppConfig.chordState.storeFileInWorkDir(file, false);
								Logger.timestampedStandardPrint("Successfully pulled file " + file.getPathInStorageDir());
							}
						} else {
							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
						}
						AppConfig.mergeResolver.pullResponseReceived(sgsf != null);
						break;
					case VIEW:
						if (sgsf != null) {
							for (SillyGitStorageFile file: sgsf) {
								Logger.timestampedStandardPrint("Successfully fetched 'view' file for " + file.getPathInStorageDir());
								AppConfig.chordState.storeFileInWorkDir(file, true);
							}
						} else {
							Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
						}
						AppConfig.mergeResolver.viewResponseReceived(sgsf != null);
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
