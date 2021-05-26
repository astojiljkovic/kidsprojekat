package servent.handler;

import app.AppConfig;
import app.DataNotOnOurNodeException;
import app.Logger;
import app.storage.FileDoesntExistStorageException;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;
import servent.message.util.MessageUtil;

public class AskGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public AskGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.ASK_GET) {
			try {
				String []fileNameAndVersion = clientMessage.getMessageText().split(" ");
				String fileName = fileNameAndVersion[0];
				int version = Integer.parseInt(fileNameAndVersion[1]);

				try {
					String localVal = AppConfig.chordState.retrieveFileFromOurStorage(fileName, version).getContent();
					TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
							fileName, localVal);
					MessageUtil.sendMessage(tgm);
				} catch (FileDoesntExistStorageException e) {
						TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
							fileName, TellGetMessage.FILE_DOESNT_EXIST_CONTENT);
					MessageUtil.sendMessage(tgm);
				} catch (DataNotOnOurNodeException e) {
					AppConfig.chordState.sendAskGetMessage(fileName, version, clientMessage.getSender());
				}

			} catch (NumberFormatException e) {
				Logger.timestampedErrorPrint("Got ask get with bad text: " + clientMessage.getMessageText());
			}
			
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}