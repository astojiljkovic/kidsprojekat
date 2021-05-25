package servent.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import app.AppConfig;
import app.Logger;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NewNodeMessage;
import servent.message.SorryMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

public class NewNodeHandler implements MessageHandler {

	private Message clientMessage;
	
	public NewNodeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NEW_NODE) {
			ServentInfo newNodeInfo = clientMessage.getSender();
			
			//check if the new node collides with another existing node.
			if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
				Message sry = new SorryMessage(AppConfig.myServentInfo, newNodeInfo);
				MessageUtil.sendMessage(sry);
				return;
			}
			
			//check if he is my predecessor
			boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
			if (isMyPred) { //if yes, prepare and send welcome message
				ServentInfo hisPred = AppConfig.chordState.getPredecessor();
				if (hisPred == null) {
					hisPred = AppConfig.myServentInfo;
				}
				
				AppConfig.chordState.setPredecessor(newNodeInfo);
				
				Map<String, String> myValues = AppConfig.chordState.getValueMap();
				Map<String, String> hisValues = new HashMap<>();
				
				int myId = AppConfig.myServentInfo.getChordId();
				int hisPredId = hisPred.getChordId();
				int newNodeId = newNodeInfo.getChordId();
				
				for (Entry<String, String> valueEntry : myValues.entrySet()) {
					int entryKey = AppConfig.chordState.chordHash(valueEntry.getKey().hashCode()); // TODO: 25.5.21. Move hashing from here
					if (hisPredId == myId) { //i am first and he is second
						if (myId < newNodeId) {
							if (entryKey <= newNodeId && entryKey > myId) {
								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
							}
						} else {
							if (entryKey <= newNodeId || entryKey > myId) {
								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
							}
						}
					}
					if (hisPredId < myId) { //my old predecesor was before me
						if (entryKey <= newNodeId) {
							hisValues.put(valueEntry.getKey(), valueEntry.getValue());
						}
					} else { //my old predecesor was after me
						if (hisPredId > newNodeId) { //new node overflow
							if (entryKey <= newNodeId || entryKey > hisPredId) {
								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
							}
						} else { //no new node overflow
							if (entryKey <= newNodeId && entryKey > hisPredId) {
								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
							}
						}
						
					}
					
				}
				for (String key : hisValues.keySet()) { //remove his values from my map
					myValues.remove(key);
				}
				AppConfig.chordState.setValueMap(myValues);
				
				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo, newNodeInfo, hisValues);
				MessageUtil.sendMessage(wm);
			} else { //if he is not my predecessor, let someone else take care of it
				ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
				NewNodeMessage nnm = new NewNodeMessage(newNodeInfo, nextNode);
				MessageUtil.sendMessage(nnm);
			}
			
		} else {
			Logger.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
		}

	}

}
