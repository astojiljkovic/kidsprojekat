package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitFile;
import app.SillyGitStorageFile;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;

public class TellGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public TellGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TELL_GET) {
			TellGetMessage tellGetMessage = (TellGetMessage) clientMessage;
			String requestedPath = tellGetMessage.getMessageText();

			SillyGitStorageFile sgsf = tellGetMessage.getSgsf();

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
