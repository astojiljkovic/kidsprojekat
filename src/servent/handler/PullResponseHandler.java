package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitStorageFile;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PullResponseMessage;

public class PullResponseHandler extends ResponseMessageHandler {

//	private Message clientMessage;
//
//	public PullResponseHandler(Message clientMessage) {
//		this.clientMessage = clientMessage;
//	}
	
	@Override
	public void run() {
		if (message.getMessageType() == MessageType.PULL_RESPONSE) {
			PullResponseMessage pullResponseMessage = (PullResponseMessage) message;
			String requestedPath = pullResponseMessage.getMessageText();

			SillyGitStorageFile sgsf = pullResponseMessage.getSgsf();

			if (sgsf == null) {
				Logger.timestampedStandardPrint("No such file with name: " + requestedPath);
			} else {
				AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir() , sgsf.getContent(), sgsf.getVersionHash());
				Logger.timestampedStandardPrint("Successfully pulled file " + sgsf);
			}
		} else {
			Logger.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
