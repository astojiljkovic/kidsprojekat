package servent.handler;

import java.util.ArrayList;
import java.util.List;

import app.*;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NewNodeMessage;
import servent.message.SorryMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

import static app.ChordState.hashForFilePath;

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
			if (AppConfig.chordState.state.isCollision(newNodeInfo.getChordId())) {
				SorryMessage sry = new SorryMessage(AppConfig.myServentInfo, newNodeInfo);
				MessageUtil.sendAndForgetMessage(sry);
				return;
			}
			
			//check if he is my predecessor
			boolean isMyPred = AppConfig.chordState.state.isKeyMine(newNodeInfo.getChordId());
			if (isMyPred) { //if yes, prepare and send welcome message
				ServentInfo hisPred = AppConfig.chordState.state.getPredecessor();
				if (hisPred == null) {
					hisPred = AppConfig.myServentInfo;
				}
				
				AppConfig.chordState.state.setPredecessor(newNodeInfo);

				System.out.println("* * * MY files * * *");
				List<String> myStoredFilePaths = AppConfig.storage.getAllStoredUnversionedFileNamesRelativeToRoot();
				List<String> hisFilePaths = new ArrayList<>();

				int myId = AppConfig.myServentInfo.getChordId();
				int hisPredId = hisPred.getChordId();
				int newNodeId = newNodeInfo.getChordId();
				
				for (String storedFilePath : myStoredFilePaths) {
					;
//					int entryKey = AppConfig.chordState.chordHash(storedFilePath.hashCode()); // TODO: 25.5.21. Move hashing from here
					int entryKey = hashForFilePath(storedFilePath);
					System.out.println("" + storedFilePath + " | " + entryKey);

					if (hisPredId == myId) { //i am first and he is second
						if (myId < newNodeId) {
							if (entryKey <= newNodeId && entryKey > myId) {
								hisFilePaths.add(storedFilePath);
							}
						} else {
							if (entryKey <= newNodeId || entryKey > myId) {
								hisFilePaths.add(storedFilePath);
							}
						}
					}
					if (hisPredId < myId) { //my old predecesor was before me
						if (entryKey <= newNodeId) {
							hisFilePaths.add(storedFilePath);
						}
					} else { //my old predecesor was after me
						if (hisPredId > newNodeId) { //new node overflow
							if (entryKey <= newNodeId || entryKey > hisPredId) {
								hisFilePaths.add(storedFilePath);
							}
						} else { //no new node overflow
							if (entryKey <= newNodeId && entryKey > hisPredId) {
								hisFilePaths.add(storedFilePath);
							}
						}
						
					}
					
				}

				System.out.println("*** His paths ***");

				// removeFileForUs his values from my storage
				List<SillyGitStorageFile> hisFiles = AppConfig.storage.removeFilesOnRelativePathsReturningGitFiles(hisFilePaths);
				for(SillyGitStorageFile file: hisFiles) {
					System.out.println("" + file.getPathInStorageDir());
				}
				
				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo, newNodeInfo, hisFiles);
				MessageUtil.sendAndForgetMessage(wm);
			} else { //if he is not my predecessor, let someone else take care of it
				ServentInfo nextNode = AppConfig.chordState.state.getNextNodeForKey(newNodeInfo.getChordId());
				NewNodeMessage nnm = new NewNodeMessage(newNodeInfo, nextNode);
				MessageUtil.sendAndForgetMessage(nnm);
			}
			
		} else {
			Logger.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
		}

	}

}
