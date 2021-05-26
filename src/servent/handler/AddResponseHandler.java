package servent.handler;

import app.AppConfig;
import app.DataNotOnOurNodeException;
import app.Logger;
import app.SillyGitStorageFile;
import app.storage.FileDoesntExistStorageException;
import servent.message.AddResponseMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;
import servent.message.util.MessageUtil;

public class AddResponseHandler implements MessageHandler {

	private Message clientMessage;

	public AddResponseHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.ADD_RESPONSE) {
			AddResponseMessage responseMessage = (AddResponseMessage) clientMessage;

			SillyGitStorageFile sgsf = responseMessage.getSgsf();
			if (sgsf == null) {
				Logger.timestampedErrorPrint("Couldn't add file - " + clientMessage.getMessageText());
			} else {
				AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
			}
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}