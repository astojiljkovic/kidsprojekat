package app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.*;

import app.git.add.AddResult;
import app.git.commit.CommitResult;
import app.git.pull.PullResult;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;
import servent.handler.AddResponseHandler;
import servent.handler.CommitResponseHandler;
import servent.handler.PullResponseHandler;
import servent.message.*;
import servent.message.util.MessageUtil;

import static app.AppConfig.*;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * 
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public class ChordState {

	public static int CHORD_SIZE;
	public static int chordHash(int value) {
		int absValue = Math.abs(value) % 5000; // TODO: 25.5.21. Fix dirty cheat for positive values
		return 61 * absValue % CHORD_SIZE;
	}
	
	private final int chordLevel; //log_2(CHORD_SIZE)
	
	private ServentInfo[] successorTable;
	private ServentInfo predecessorInfo;
	
	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;
	
//	private Map<String, String> valueMap;
	
	public ChordState() {
		int tmpChordLvl = 1;
		int tmp = CHORD_SIZE;
		while (tmp != 2) {
			if (tmp % 2 != 0) { //not a power of 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			tmpChordLvl++;
		}
		this.chordLevel = tmpChordLvl;

		successorTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}
		
		predecessorInfo = null;
//		valueMap = new HashMap<>();
		allNodeInfo = new ArrayList<>();
	}

	public static int hashForFilePath(String pathInDir) {
		return chordHash(Path.of(pathInDir).getName(0).toString().hashCode());
	}

	/**
	 * This should be called once after we get <code>WELCOME</code> message.
	 * It sets up our initial value map and our first successor so we can send <code>UPDATE</code>.
	 * It also lets bootstrap know that we did not collide.
	 */
	public void init(WelcomeMessage welcomeMsg) {
		//set a temporary pointer to next node, for sending of update message
		successorTable[0] = welcomeMsg.getSender();
//		this.valueMap = welcomeMsg.getValues();

		for(SillyGitStorageFile sgsf: welcomeMsg.getFiles()) {
			try {
				storage.add(sgsf.getPathInStorageDir(), sgsf.getContent());
			} catch (FileAlreadyAddedStorageException e) {
				Logger.timestampedErrorPrint("Cannot add file to storage on Welcome message: " + sgsf);
			}
		}
		
		//tell bootstrap this node is not a collider
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + myServentInfo.getNetworkLocation().getIp() + "\n" + myServentInfo.getNetworkLocation().getPort() + "\n");
			
			bsWriter.flush();
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}

	public ServentInfo getSuccessorInfo() {
		return successorTable[0];
	}
	
	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}
	
	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}
	
	public boolean isCollision(int chordId) {
		if (chordId == myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if we are the owner of the specified key.
	 */
	public boolean isKeyMine(int key) {
		if (predecessorInfo == null) {
			return true;
		}
		
		int predecessorChordId = predecessorInfo.getChordId();
		int myChordId = myServentInfo.getChordId();
		
		if (predecessorChordId < myChordId) { //no overflow
			if (key <= myChordId && key > predecessorChordId) {
				return true;
			}
		} else { //overflow
			if (key <= myChordId || key > predecessorChordId) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Main chord operation - find the nearest node to hop to to find a specific key.
	 * We have to take a value that is smaller than required to make sure we don't overshoot.
	 * We can only be certain we have found the required node when it is our first next node.
	 */
	public ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return myServentInfo;
		}
		
		//normally we start the search from our first successor
		int startInd = 0;
		
		//if the key is smaller than us, and we are not the owner,
		//then all nodes up to CHORD_SIZE will never be the owner,
		//so we start the search from the first item in our table after CHORD_SIZE
		//we know that such a node must exist, because otherwise we would own this key
		if (key < myServentInfo.getChordId()) {
			int skip = 1;
			while (successorTable[skip].getChordId() > successorTable[startInd].getChordId()) {
				startInd++;
				skip++;
			}
		}
		
		int previousId = successorTable[startInd].getChordId();
		
		for (int i = startInd + 1; i < successorTable.length; i++) {
			if (successorTable[i] == null) {
				Logger.timestampedErrorPrint("Couldn't find successor for " + key);
				break;
			}
			
			int successorId = successorTable[i].getChordId();
			
			if (successorId >= key) {
				return successorTable[i-1];
			}
			if (key > previousId && successorId < previousId) { //overflow
				return successorTable[i-1];
			}
			previousId = successorId;
		}
		//if we have only one node in all slots in the table, we might get here
		//then we can return any item
		return successorTable[0];
	}

	private void updateSuccessorTable() {
		//first node after me has to be successorTable[0]
		
		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTable[0] = currentNode;
		
		int currentIncrement = 2;
		
		ServentInfo previousNode = myServentInfo;
		
		//i is successorTable index
		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
			//we are looking for the node that has larger chordId than this
			int currentValue = (myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;
			
			int currentId = currentNode.getChordId();
			int previousId = previousNode.getChordId();
			
			//this loop needs to skip all nodes that have smaller chordId than currentValue
			while (true) {
				if (currentValue > currentId) {
					//before skipping, check for overflow
					if (currentId > previousId || currentValue < previousId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				} else { //node id is larger
					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
					int nextNodeId = nextNode.getChordId();
					//check for overflow
					if (nextNodeId < currentId && currentValue <= nextNodeId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				}
			}
		}
		
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 * 
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);
		
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}
			
		});
		
		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();
		
		int myId = myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}
		
		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size()-1);
		} else {
			predecessorInfo = newList.get(newList.size()-1);
		}
		
		updateSuccessorTable();
	}

	//Add
	public Optional<AddResult> addFileFromMyWorkDir(String path) throws FileNotFoundException {

		//A list contains either only one file, or list of files the same dir
		List<SillyGitFile> sillyGitFiles = workDirectory.getFileForPath(path);
		SillyGitFile referenceFile = sillyGitFiles.get(0);

		int key = hashForFilePath(referenceFile.getPathInWorkDir());
		System.out.println("Dodajemo fajl " + path + " a hash mu je :" + key);

		if (isKeyMine(key)) { //TODO: storage treba da odluci za kljuc
			ArrayList<String> failedPaths = new ArrayList<>();
			ArrayList<SillyGitStorageFile> successes = new ArrayList<>();

			for (SillyGitFile sgf : sillyGitFiles) {
				try {
					SillyGitStorageFile sgsf = AppConfig.storage.add(sgf.getPathInWorkDir(), sgf.getContent());
					workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
					successes.add(sgsf);
				} catch (FileAlreadyAddedStorageException e) {
					failedPaths.addAll(e.getPath());
				}
			}

			return Optional.of(new AddResult(failedPaths, successes));
		} else {
			sendAddFilesForMe(sillyGitFiles);
			return Optional.empty();
		}
	}

	public void addFileForSomeoneElse(List<SillyGitFile> sillyGitFiles, AddMessage receivedMessage) {
		int key = hashForFilePath(sillyGitFiles.get(0).getPathInWorkDir());
		if (isKeyMine(key)) { //TODO: storage treba da odluci za kljuc
			ArrayList<String> failedPaths = new ArrayList<>();
			ArrayList<SillyGitStorageFile> successes = new ArrayList<>();

			for (SillyGitFile sgf : sillyGitFiles) {
				try {
					SillyGitStorageFile sgsf = AppConfig.storage.add(sgf.getPathInWorkDir(), sgf.getContent());
					successes.add(sgsf);
				} catch (FileAlreadyAddedStorageException e) {
					Logger.timestampedErrorPrint("Cannot add file - File already exists: " + e);
					failedPaths.addAll(e.getPath());
				}
			}

			AddResult result = new AddResult(failedPaths, successes);
			sendAddResponseMessage(result, receivedMessage);
		} else {
			forwardAddFileMessage(sillyGitFiles, receivedMessage);
		}
	}

	private void forwardAddFileMessage(List<SillyGitFile> sillyGitFiles, AddMessage message) {
		int key = hashForFilePath(sillyGitFiles.get(0).getPathInWorkDir());

		ServentInfo nextNode = getNextNodeForKey(key);

		MessageUtil.sendAndForgetMessage(message.newMessageFor(nextNode));
	}

	private void sendAddFilesForMe(List<SillyGitFile> sillyGitFiles) {
		int key = hashForFilePath(sillyGitFiles.get(0).getPathInWorkDir());

		ServentInfo nextNode = getNextNodeForKey(key);

		AddMessage addMessage = new AddMessage(myServentInfo, nextNode, sillyGitFiles);
		MessageUtil.sendTrackedMessageAwaitingResponse(addMessage, new AddResponseHandler());
	}

	private void sendAddResponseMessage(AddResult result, AddMessage receivedMessage) {
		AddResponseMessage responseMessage = new AddResponseMessage(myServentInfo, receivedMessage.getSender(), result);
		responseMessage.copyContextFrom(receivedMessage);

		MessageUtil.sendAndForgetMessage(responseMessage);
	}
	
	/**
	 * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
	 * @return <ul>
	 *			<li>The value, if we have it</li>
	 *			<li>-1 if we own the key, but there is nothing there</li>
	 *			<li>-2 if we asked someone else</li>
	 *		   </ul>
	 */
	//pull

	/**
	 *
	 * @param filePath
	 * @param version
	 * @param pullType
	 * @return PullResult will contain empty arrays if pull is forwarded to system for remote fetch
	 */
	public Optional<PullResult> pullFileForUs(String filePath, int version, PullType pullType) {
		try {
			PullResult pullResult = retrieveFilesFromOurStorage(filePath, version);

			for(SillyGitStorageFile sgsf: pullResult.getSuccesses()) {
				storeFileInWorkDir(sgsf, pullType == PullType.VIEW);
			}

			return Optional.of(pullResult);
		} catch (DataNotOnOurNodeException e) {
			sendPullMessageForMe(filePath, version, pullType);
			return Optional.empty();
		}
	}

	public void storeFileInWorkDir(SillyGitStorageFile sgsf, boolean isTmp) {
		String workdirFilePath = sgsf.getPathInStorageDir();
		if (isTmp) {
			workdirFilePath += ".tmp";
		}
		workDirectory.addFile(workdirFilePath, sgsf.getContent(), sgsf.getVersionHash());
	}

	public void pullFileForSomeoneElse(PullMessage requestMessage) {
		try {
			PullResult pullResult = retrieveFilesFromOurStorage(requestMessage.getFileName(), requestMessage.getVersion());
			sendPullResponseMessage(pullResult, requestMessage);
		}
		catch (DataNotOnOurNodeException e) {
			forwardPullMessage(requestMessage);
		}
	}

	private PullResult retrieveFilesFromOurStorage(String fileName, int version) throws DataNotOnOurNodeException {
		int key = hashForFilePath(fileName);

		if (isKeyMine(key)) {
			Storage.GetResult storageResult = storage.get(fileName, version);
			return new PullResult(storageResult.getFailedPaths(), storageResult.getSuccesses());
		}

		throw new DataNotOnOurNodeException();
	}

	private void forwardPullMessage(PullMessage originalMessage) {
		int key = chordHash(originalMessage.getFileName().hashCode());
		ServentInfo nextNode = getNextNodeForKey(key);

		PullMessage messageToForward = originalMessage.newMessageFor(nextNode);
		MessageUtil.sendAndForgetMessage(messageToForward);
	}

	private void sendPullMessageForMe(String filePath, int version, PullType pullType) {
		int key = chordHash(filePath.hashCode());
		ServentInfo nextNode = getNextNodeForKey(key);

		PullMessage message = new PullMessage(myServentInfo, nextNode, filePath, version);

		MessageUtil.sendTrackedMessageAwaitingResponse(message, new PullResponseHandler(pullType));
	}

	private void sendPullResponseMessage(PullResult pullResult, PullMessage askMessage) {
		PullResponseMessage responseMessage = new PullResponseMessage(myServentInfo, askMessage.getSender(), askMessage.getFileName(), pullResult);
		responseMessage.copyContextFrom(askMessage);
		MessageUtil.sendAndForgetMessage(responseMessage);
	}

	//Remove

	public void remove(String removePath) {
		int key = chordHash(removePath.hashCode());

		if (isKeyMine(key)) {
			storage.removeFilesOnRelativePathsReturningGitFiles(List.of(removePath));
			workDirectory.removeFileForPath(removePath);
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			RemoveMessage rm = new RemoveMessage(myServentInfo, nextNode, removePath);
			MessageUtil.sendAndForgetMessage(rm);
		}
	}

	//Commit

	public Optional<CommitResult> commitFileFromMyWorkDir(String filePath, boolean force) {
		ArrayList<String> failedPaths = new ArrayList<>();
		ArrayList<SillyGitStorageFile> successes = new ArrayList<>();
		ArrayList<SillyGitFile> conflicts = new ArrayList<>();

		List<SillyGitFile> filesInWorkDir;
		try {
			filesInWorkDir = workDirectory.getFileForPath(filePath);
		} catch (FileNotFoundException e) {
			failedPaths.add(filePath);
			return Optional.of(new CommitResult(failedPaths, successes, conflicts));
		}

		SillyGitFile referenceFile = filesInWorkDir.get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		if (isKeyMine(key)) {

			for (SillyGitFile fileInWorkDir : filesInWorkDir) {
				try {
					if (fileInWorkDir.getStorageHash().isEmpty()) {
						failedPaths.add(fileInWorkDir.getPathInWorkDir());
						continue;
					}
					String hash = fileInWorkDir.getStorageHash().get();
					SillyGitStorageFile sgfs = storage.commit(fileInWorkDir.getPathInWorkDir(), fileInWorkDir.getContent(), hash, force);
					workDirectory.addFile(sgfs.getPathInStorageDir(), sgfs.getContent(), sgfs.getVersionHash());
					successes.add(sgfs);
				} catch (FileDoesntExistStorageException e) {
					failedPaths.add(fileInWorkDir.getPathInWorkDir());
				} catch (CommitConflictStorageException e) {
					conflicts.add(fileInWorkDir);
				}
			}
			return Optional.of(new CommitResult(failedPaths, successes, conflicts));
		} else {
			sendCommitMessageForUs(filesInWorkDir, myServentInfo, force);
			return Optional.empty();
		}
	}

	public void commitFileFromSomeoneElse(CommitMessage message) {
		SillyGitFile referenceFile = message.getFilesToCommit().get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		if (isKeyMine(key)) {
			ArrayList<String> failedPaths = new ArrayList<>();
			ArrayList<SillyGitStorageFile> successes = new ArrayList<>();
			ArrayList<SillyGitFile> conflicts = new ArrayList<>();

			for (SillyGitFile sgf: message.getFilesToCommit()) {
				if (sgf.getStorageHash().isEmpty()) {
					failedPaths.add(sgf.getPathInWorkDir());
				} else {
					try {
						String hash = sgf.getStorageHash().get();
						SillyGitStorageFile sgsf = storage.commit(sgf.getPathInWorkDir(), sgf.getContent(), hash, message.getIsForce());
						successes.add(sgsf);
					} catch (FileDoesntExistStorageException e) {
						Logger.timestampedErrorPrint("Commit unsuccessful, recording failure " + sgf.getPathInWorkDir());
						failedPaths.add(sgf.getPathInWorkDir());
					} catch (CommitConflictStorageException e) {
						Logger.timestampedErrorPrint("Commit unsuccessful, recording conflict " + sgf.getPathInWorkDir());
						conflicts.add(sgf);
					}
				}
			}

			CommitResult commitResult = new CommitResult(failedPaths, successes, conflicts);
			sendCommitResponseMessage(message, commitResult);
		} else {
			forwardCommitMessageForSomeoneElse(message);
		}
	}

	private void sendCommitMessageForUs(List<SillyGitFile> filesInWorkDir, ServentInfo myServentInfo, boolean isCommitResolution) {
		SillyGitFile referenceFile = filesInWorkDir.get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		ServentInfo nextNode = getNextNodeForKey(key);
		CommitMessage cm = new CommitMessage(myServentInfo, nextNode, filesInWorkDir, isCommitResolution);

		MessageUtil.sendTrackedMessageAwaitingResponse(cm, new CommitResponseHandler(isCommitResolution));
	}

	private void forwardCommitMessageForSomeoneElse(CommitMessage originalMessage) {
		SillyGitFile referenceFile = originalMessage.getFilesToCommit().get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		ServentInfo nextNode = getNextNodeForKey(key);

		CommitMessage cm = originalMessage.newMessageFor(nextNode);
		MessageUtil.sendAndForgetMessage(cm);
	}

	//sgsf can be null
	private void sendCommitResponseMessage(CommitMessage message, CommitResult commitResult) {
		CommitResponseMessage cm = new CommitResponseMessage(myServentInfo, message.getSender(), commitResult);
		cm.copyContextFrom(message);
		MessageUtil.sendAndForgetMessage(cm);
	}

	public Optional<PullResult> getViewFile(String resolvingConflictPath) {
		return pullFileForUs(resolvingConflictPath, Storage.LATEST_STORAGE_FILE_VERSION, PullType.VIEW);
	}
}
