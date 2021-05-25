package servent.handler;

import app.AppConfig;
import app.Logger;
import app.ServentInfo;
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

				ServentInfo requester = clientMessage.getSender();

				String localVal = AppConfig.chordState.getLocalValue(fileName);

				if (localVal.equals("-1")) {
					TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
							fileName, "FAJL_NE_POSTOJI");
					MessageUtil.sendMessage(tgm);
					return;
				}

				if (localVal.equals("-2")) {
					AppConfig.chordState.sendAskGetMessage(fileName, clientMessage.getSender());
					return;
				}

				TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
						fileName, localVal);
				MessageUtil.sendMessage(tgm);

//				int key = Integer.parseInt(clientMessage.getMessageText()); //TODO: pobrisi kad proverimo
//				String fileName = clientMessage.getMessageText();
//
//				if (AppConfig.chordState.isKeyMine(key)) {
//					Map<Integer, Integer> valueMap = AppConfig.chordState.getValueMap();
//					int value = -1;
//
//					if (valueMap.containsKey(key)) {
//						value = valueMap.get(key);
//					}
//
//					TellGetMessage tgm = new TellGetMessage(AppConfig.myServentInfo, clientMessage.getSender(),
//															key, value);
//					MessageUtil.sendMessage(tgm);
//				} else {
//					ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);
//					AskGetMessage agm = new AskGetMessage(clientMessage.getSender(), nextNode, clientMessage.getMessageText());
//					MessageUtil.sendMessage(agm);
//				}
			} catch (NumberFormatException e) {
				Logger.timestampedErrorPrint("Got ask get with bad text: " + clientMessage.getMessageText());
			}
			
		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}