package servent.handler;

import app.*;
import app.storage.SillyGitStorageFile;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

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

				AppConfig.chordState.state.addNodes(List.of(newNodeInfo), Collections.emptyList());
				
//				AppConfig.chordState.state.setPredecessor(newNodeInfo);

				//System is starting, we don't have succ (we are first node)
//				if (AppConfig.chordState.state.getClosestSuccessor() == null) {
//					AppConfig.chordState.state.setSuccessors(List.of(newNodeInfo));
//				}

				System.out.println("* * * MY files * * *");
				List<String> myStoredFilePaths = AppConfig.storage.getAllStoredUnversionedFileNamesRelativeToStorageRoot();
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

				//TODO: this looks sketchy
				List<ServentInfo> mySuccs = new ArrayList<>(Arrays.asList(AppConfig.chordState.state.getSuccessors()));

				mySuccs.add(0, AppConfig.myServentInfo);

				if (mySuccs.size() > ChordState.State.MAX_SUCCESSORS) {
					mySuccs.remove(mySuccs.size() - 1);
				}

				//TODO: don't return null successors
				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo, newNodeInfo, hisFiles, mySuccs.stream().filter(Objects::nonNull).collect(Collectors.toList()));
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
