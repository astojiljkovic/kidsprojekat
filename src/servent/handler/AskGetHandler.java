package servent.handler;

import app.AppConfig;
import app.DataNotOnOurNodeException;
import app.Logger;
import app.ServentInfo;
import app.storage.FileDoesntExistException;
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
				String fileName = clientMessage.getMessageText();

//				ServentInfo requester = clientMessage.getSender();

//				String localVal = null;
				try {
					String localVal = AppConfig.chordState.getLocalValue(fileName);
					TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
							fileName, localVal);
					MessageUtil.sendMessage(tgm);
				} catch (FileDoesntExistException e) {
						TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
							fileName, "FAJL_NE_POSTOJI");
					MessageUtil.sendMessage(tgm);
//					e.printStackTrace();
				} catch (DataNotOnOurNodeException e) {
					AppConfig.chordState.sendAskGetMessage(fileName, clientMessage.getSender());
//					e.printStackTrace();
				}

//				if (localVal.equals("-1")) {
//					TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
//							fileName, "FAJL_NE_POSTOJI");
//					MessageUtil.sendMessage(tgm);
//					return;
//				}

//				if (localVal.equals("-2")) {
//					AppConfig.chordState.sendAskGetMessage(fileName, clientMessage.getSender());
//					return;
//				}

//				TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
//						fileName, localVal);
//				MessageUtil.sendMessage(tgm);

			} catch (NumberFormatException e) {
				Logger.timestampedErrorPrint("Got ask get with bad text: " + clientMessage.getMessageText());
			}
			
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}