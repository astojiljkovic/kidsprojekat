package servent.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import app.AppConfig;
import app.Logger;
import app.ServentInfo;
import app.SillyGitFile;
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

				List<SillyGitFile> myValues = AppConfig.storage.getAllFiles();
				List<SillyGitFile> hisValues = new ArrayList<>();

				int myId = AppConfig.myServentInfo.getChordId();
				int hisPredId = hisPred.getChordId();
				int newNodeId = newNodeInfo.getChordId();
				
				for (SillyGitFile gitFile : myValues) {
					int entryKey = AppConfig.chordState.chordHash(gitFile.getPathInWorkDir().hashCode()); // TODO: 25.5.21. Move hashing from here
					if (hisPredId == myId) { //i am first and he is second
						if (myId < newNodeId) {
							if (entryKey <= newNodeId && entryKey > myId) {
								hisValues.add(gitFile);
							}
						} else {
							if (entryKey <= newNodeId || entryKey > myId) {
								hisValues.add(gitFile);
							}
						}
					}
					if (hisPredId < myId) { //my old predecesor was before me
						if (entryKey <= newNodeId) {
							hisValues.add(gitFile);
						}
					} else { //my old predecesor was after me
						if (hisPredId > newNodeId) { //new node overflow
							if (entryKey <= newNodeId || entryKey > hisPredId) {
								hisValues.add(gitFile);
							}
						} else { //no new node overflow
							if (entryKey <= newNodeId && entryKey > hisPredId) {
								hisValues.add(gitFile);
							}
						}
						
					}
					
				}

				// remove hist values from my list
				myValues = myValues.stream().filter(sillyGitFile -> {
					return hisValues.stream().noneMatch(sillyGitFile1 -> sillyGitFile1.getPathInWorkDir().equals(sillyGitFile.getPathInWorkDir()));
				}).collect(Collectors.toList());

				AppConfig.storage.setAllFiles(myValues);

				Map<String, String> mapToSend = new HashMap<>(); //TODO: update WelcomeMsg
				for(SillyGitFile sgf: hisValues) {
					mapToSend.put(sgf.getPathInWorkDir(), sgf.getContent());
				}
				
				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo, newNodeInfo, mapToSend);
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
