package app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import app.git.add.AddResult;
import app.git.commit.CommitResult;
import app.git.pull.RemoveResult;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;
import servent.handler.data.AddResponseHandler;
import servent.handler.data.CommitResponseHandler;
import servent.handler.data.PullResponseHandler;
import servent.handler.data.RemoveResponseHandler;
import servent.message.*;
import servent.message.data.*;
import servent.message.util.MessageUtil;

import javax.print.attribute.standard.Severity;

import static app.AppConfig.*;
import static java.lang.System.exit;

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
		System.out.println("Hash " + value);
		int absValue = Math.abs(value) % 50000; // TODO: 25.5.21. Fix dirty cheat for positive values
		return 61 * absValue % CHORD_SIZE;
	}
	
	private final int chordLevel; //log_2(CHORD_SIZE)
	
	private ServentInfo[] successorTable;
	private ServentInfo predecessorInfo;
	
	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;
	
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
		allNodeInfo = new ArrayList<>();
	}

	public static int hashForFilePath(String pathInDir) {
		try {
			byte []shaBytes = MessageDigest.getInstance("SHA-1").digest(pathInDir.getBytes());
			ByteBuffer wrapped = ByteBuffer.wrap(shaBytes);
//			wrapped.getInt();
//			return wrapped.getInt();
//			return chordHash(Path.of(pathInDir).getName(0).toString().hashCode());
			return chordHash(wrapped.getInt());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		exit(1);
		return -1;
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

//		System.out.println("---Current succ table---" );
//
//		for(ServentInfo info: successorTable) {
//			System.out.println("" + info);
//		}

		List<Integer> values = new ArrayList<>();
		for(int i = 0; i < chordLevel; i++) {
			values.add((int) (myServentInfo.getChordId() + Math.pow(2, i)) % CHORD_SIZE);
		}

		//[12(raketa)|localhost:1200]
		//[54(raketa)|localhost:1100]
		List<ServentInfo> succis = new ArrayList<>();
		for(int value: values) {
			List<ServentInfo> bigger = allNodeInfo.stream().filter(serventInfo -> serventInfo.getChordId() >= value).sorted(Comparator.comparingInt(ServentInfo::getChordId)).collect(Collectors.toList());
			List<ServentInfo> smaller = allNodeInfo.stream().filter(serventInfo -> serventInfo.getChordId() < value).sorted(Comparator.comparingInt(ServentInfo::getChordId)).collect(Collectors.toList());

			Optional<ServentInfo> firstBigger = bigger.stream().findFirst();
			if (firstBigger.isPresent()) {
				succis.add(firstBigger.get());
			} else {
				ServentInfo smallestSmall = smaller.get(0);
				succis.add(smallestSmall);
			}
		}

		for(int i = 0; i < chordLevel; i++) {
			successorTable[i] = succis.get(i);
		}

//		int currentNodeIndex = 0;
//		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
//		successorTable[0] = currentNode;
//
//		int currentIncrement = 2;
//
//		ServentInfo previousNode = myServentInfo;
//
//		//i is successorTable index
//		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
//			//we are looking for the node that has larger chordId than this
//			int currentValue = (myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;
//
//			int currentId = currentNode.getChordId();
//			int previousId = previousNode.getChordId();
//
//			//this loop needs to skip all nodes that have smaller chordId than currentValue
//			while (true) {
//				if (currentValue > currentId) {
//					//before skipping, check for overflow
//					if (currentId > previousId || currentValue < previousId) {
//						//try same value with the next node
//						previousId = currentId;
//						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
//						currentNode = allNodeInfo.get(currentNodeIndex);
//						currentId = currentNode.getChordId();
//					} else {
//						System.out.println("Dodaje se" );
//						System.out.println("successorTable[" + i + "]" + "= " + currentNode + " | " + "currentValue = " + currentValue + " | currentId = " + currentId + " | previousId = " + previousId + " | currentValue = " + currentValue);
//
//						successorTable[i] = currentNode;
//						break;
//					}
//				} else { //node id is larger
//					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
//					int nextNodeId = nextNode.getChordId();
//					//check for overflow
//					if (nextNodeId < currentId && currentValue <= nextNodeId) {
//						//try same value with the next node
//						previousId = currentId;
//						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
//						currentNode = allNodeInfo.get(currentNodeIndex);
//						currentId = currentNode.getChordId();
//					} else {
//						System.out.println("Dodaje se" );
//						System.out.println("successorTable[" + i + "]" + "= " + currentNode + " | " + "currentValue = " + currentValue + " | currentId = " + currentId + " | previousId = " + previousId + " | currentValue = " + currentValue);
//						successorTable[i] = currentNode;
//						break;
//					}
//				}
//			}
//		}

		System.out.println("New succ table" );

		for(int i = 0; i < successorTable.length; i++) {
			System.out.println("" + (((int) (myServentInfo.getChordId() + Math.pow(2, i))) % CHORD_SIZE) + " " + successorTable[i]);
		}
//		for(ServentInfo info: successorTable) {
//
//			System.out.println("" + info);
//		}
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 * 
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);

		Set<ServentInfo> servents = new HashSet<>(allNodeInfo);

		allNodeInfo = new ArrayList<>(servents);
		
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}
			
		});

		
		List<ServentInfo> biggerIdNodes = new ArrayList<>();
		List<ServentInfo> smallerIdNodes = new ArrayList<>();

		System.out.println("---- All current nodes ----");

		int myId = myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			System.out.println("" + serventInfo);
			if (serventInfo.getChordId() < myId) {
				smallerIdNodes.add(serventInfo);
			} else {
				biggerIdNodes.add(serventInfo);
			}
		}
		
		allNodeInfo.clear();
		allNodeInfo.addAll(biggerIdNodes);
		allNodeInfo.addAll(smallerIdNodes);
		if (smallerIdNodes.size() > 0) {
			predecessorInfo = smallerIdNodes.get(smallerIdNodes.size()-1);
		} else {
			predecessorInfo = biggerIdNodes.get(biggerIdNodes.size()-1);
		}
		System.out.println("predecessor " + predecessorInfo);
		System.out.println("succi " + getSuccessorInfo());
		
		updateSuccessorTable();
	}


	//Add

	public Optional<AddResult> addFileFromMyWorkDir(String path) throws FileNotFoundException {

		//A list contains either only one file, or list of files in the same dir
		List<SillyGitFile> sillyGitFiles = workDirectory.getFilesForPath(path);

		System.out.println("paths found");
		for (SillyGitFile sgf: sillyGitFiles) {
			System.out.println("" + sgf.getPathInWorkDir());
		}

		Optional<AddResult> resultOpt = addSillyGitFilesToLocalStorage(sillyGitFiles);

		if(resultOpt.isEmpty()) {
			sendAddFilesForMe(sillyGitFiles);
		} else {
			AddResult result = resultOpt.get();
			for (SillyGitStorageFile sillyGitStorageFile : result.getSuccesses()) {
				workDirectory.addFile(sillyGitStorageFile.getPathInStorageDir(), sillyGitStorageFile.getContent(), sillyGitStorageFile.getVersionHash());
			}
		}

		return resultOpt;
	}

	private Optional<AddResult> addSillyGitFilesToLocalStorage(List<SillyGitFile> sgfs) {
		SillyGitFile referenceFile = sgfs.get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		if (isKeyMine(key)) {
			ArrayList<String> failedPaths = new ArrayList<>();
			ArrayList<SillyGitStorageFile> successes = new ArrayList<>();

			for (SillyGitFile sgf : sgfs) {
				try {
					SillyGitStorageFile sgsf = AppConfig.storage.add(sgf.getPathInWorkDir(), sgf.getContent());
					successes.add(sgsf);
				} catch (FileAlreadyAddedStorageException e) {
					failedPaths.addAll(e.getPath());
				}
			}

			return Optional.of(new AddResult(failedPaths, successes));
		} else {
			return Optional.empty();
		}
	}

	public void addFileForSomeoneElse(List<SillyGitFile> sillyGitFiles, AddMessage receivedMessage) {
		Optional<AddResult> resultOpt = addSillyGitFilesToLocalStorage(sillyGitFiles);

		if(resultOpt.isEmpty()) {
			forwardAddFileMessage(sillyGitFiles, receivedMessage);
		} else {
			AddResult result = resultOpt.get();
			sendAddResponseMessage(result, receivedMessage);
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
	

	//Pull

	public Optional<RemoveResult> pullFileForUs(String filePath, int version, PullType pullType) {
		Optional<RemoveResult> pullResultOptional = retrieveFilesFromOurStorage(filePath, version);

		if(pullResultOptional.isEmpty()) {
			sendPullMessageForMe(filePath, version, pullType);
		} else {
			for(SillyGitStorageFile sgsf: pullResultOptional.get().getSuccesses()) {
				storeFileInWorkDir(sgsf, pullType == PullType.VIEW);
			}
		}

		return pullResultOptional;
	}

	public void storeFileInWorkDir(SillyGitStorageFile sgsf, boolean isTmp) {
		String workdirFilePath = sgsf.getPathInStorageDir();
		if (isTmp) {
			workdirFilePath += ".tmp";
		}
		workDirectory.addFile(workdirFilePath, sgsf.getContent(), sgsf.getVersionHash());
	}

	public void pullFileForSomeoneElse(PullMessage requestMessage) {
		Optional<RemoveResult> pullResultOpt = retrieveFilesFromOurStorage(requestMessage.getFileName(), requestMessage.getVersion());

		if(pullResultOpt.isEmpty()) {
			forwardPullMessage(requestMessage);
		} else {
			RemoveResult removeResult = pullResultOpt.get();
			sendPullResponseMessage(removeResult, requestMessage);
		}
	}

	private Optional<RemoveResult> retrieveFilesFromOurStorage(String fileName, int version) {
		int key = hashForFilePath(fileName);

		if (isKeyMine(key)) {
			Storage.GetResult storageResult = storage.get(fileName, version);
			return Optional.of(new RemoveResult(storageResult.getFailedPaths(), storageResult.getSuccesses()));
		}

		return Optional.empty();
	}

	private void forwardPullMessage(PullMessage originalMessage) {
		int key = hashForFilePath(originalMessage.getFileName());
		ServentInfo nextNode = getNextNodeForKey(key);

		PullMessage messageToForward = originalMessage.newMessageFor(nextNode);
		MessageUtil.sendAndForgetMessage(messageToForward);
	}

	private void sendPullMessageForMe(String filePath, int version, PullType pullType) {
		int key = hashForFilePath(filePath);
		ServentInfo nextNode = getNextNodeForKey(key);

		PullMessage message = new PullMessage(myServentInfo, nextNode, filePath, version);

		MessageUtil.sendTrackedMessageAwaitingResponse(message, new PullResponseHandler(pullType));
	}

	private void sendPullResponseMessage(RemoveResult removeResult, PullMessage askMessage) {
		PullResponseMessage responseMessage = new PullResponseMessage(myServentInfo, askMessage.getSender(), askMessage.getFileName(), removeResult);
		responseMessage.copyContextFrom(askMessage);
		MessageUtil.sendAndForgetMessage(responseMessage);
	}

	//Remove

	public Optional<List<SillyGitStorageFile>> removeFilesForUs(String removePath) {

		Optional<List<SillyGitStorageFile>> locallyRemovedOpt = removeFilesFromOurStorage(removePath);

		if(locallyRemovedOpt.isEmpty()) {
			sendRemoveMessageForUs(removePath);
		} else {
			for(SillyGitStorageFile sgfs: locallyRemovedOpt.get()) {
				workDirectory.removeFileForPath(sgfs.getPathInStorageDir());
			}
		}

		return locallyRemovedOpt;
	}

	private Optional<List<SillyGitStorageFile>> removeFilesFromOurStorage(String removePath) {
		int key = hashForFilePath(removePath);

		if (isKeyMine(key)) {
			List<SillyGitStorageFile> removedFiles = storage.removeFilesOnRelativePathsReturningGitFiles(List.of(removePath));
			return Optional.of(removedFiles);
		}
		return Optional.empty();
	}

	public void removeFileFromSomeoneElse(RemoveMessage message) {
		Optional<List<SillyGitStorageFile>> removeResultOptional = removeFilesFromOurStorage(message.getRemovePath());

		if(removeResultOptional.isEmpty()) {
			forwardRemoveMessageForSomeoneElse(message);
		} else {
			sendRemoveResponseMessage(message, removeResultOptional.get());
		}
	}

	private void sendRemoveMessageForUs(String removePath) {
		int key = hashForFilePath(removePath);

		ServentInfo nextNode = getNextNodeForKey(key);
		RemoveMessage rm = new RemoveMessage(myServentInfo, nextNode, removePath);

		MessageUtil.sendTrackedMessageAwaitingResponse(rm, new RemoveResponseHandler());
	}

	private void forwardRemoveMessageForSomeoneElse(RemoveMessage originalMessage) {
		int key = hashForFilePath(originalMessage.getRemovePath());

		ServentInfo nextNode = getNextNodeForKey(key);

		RemoveMessage rm = originalMessage.newMessageFor(nextNode);
		MessageUtil.sendAndForgetMessage(rm);
	}

	private void sendRemoveResponseMessage(RemoveMessage message, List<SillyGitStorageFile> removedFiles) {
		RemoveResponseMessage rm = new RemoveResponseMessage(myServentInfo, message.getSender(), message.getRemovePath(), removedFiles);
		rm.copyContextFrom(message);
		MessageUtil.sendAndForgetMessage(rm);
	}

	//Commit

	public Optional<CommitResult> commitFileFromMyWorkDir(String filePath, boolean force) {

		List<SillyGitFile> filesInWorkDir;
		try {
			filesInWorkDir = workDirectory.getFilesForPath(filePath);
		} catch (FileNotFoundException e) {
			return Optional.of(new CommitResult(List.of(filePath), Collections.emptyList(), Collections.emptyList()));
		}

		Optional<CommitResult> commitResultOptional = commitFilesToOurStorage(filesInWorkDir, force);

		if(commitResultOptional.isEmpty()) {
			sendCommitMessageForUs(filesInWorkDir, myServentInfo, force);
		} else {
			CommitResult result = commitResultOptional.get();
			for(SillyGitStorageFile sgfs: result.getSuccesses()) {
				workDirectory.addFile(sgfs.getPathInStorageDir(), sgfs.getContent(), sgfs.getVersionHash());
			}
		}

		return commitResultOptional;
	}

	private Optional<CommitResult> commitFilesToOurStorage(List<SillyGitFile> sillyGitFiles, boolean force) {
		ArrayList<String> failedPaths = new ArrayList<>();
		ArrayList<SillyGitStorageFile> successes = new ArrayList<>();
		ArrayList<SillyGitFile> conflicts = new ArrayList<>();

		SillyGitFile referenceFile = sillyGitFiles.get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		if (isKeyMine(key)) {
			for (SillyGitFile sillyGitFile : sillyGitFiles) {
				try {
					if (sillyGitFile.getStorageHash().isEmpty()) {
						failedPaths.add(sillyGitFile.getPathInWorkDir());
						continue;
					}
					String hash = sillyGitFile.getStorageHash().get();
					SillyGitStorageFile sgfs = storage.commit(sillyGitFile.getPathInWorkDir(), sillyGitFile.getContent(), hash, force);
					successes.add(sgfs);
				} catch (FileDoesntExistStorageException e) {
					Logger.timestampedErrorPrint("Commit unsuccessful, recording failure " + sillyGitFile.getPathInWorkDir());
					failedPaths.add(sillyGitFile.getPathInWorkDir());
				} catch (CommitConflictStorageException e) {
					Logger.timestampedErrorPrint("Commit unsuccessful, recording conflict " + sillyGitFile.getPathInWorkDir());
					conflicts.add(sillyGitFile);
				}
			}

			return Optional.of(new CommitResult(failedPaths, successes, conflicts));
		}

		return Optional.empty();
	}

	public void commitFileFromSomeoneElse(CommitMessage message) {
		Optional<CommitResult> commitResultOptional = commitFilesToOurStorage(message.getFilesToCommit(), message.getIsForce());

		if(commitResultOptional.isEmpty()) {
			forwardCommitMessageForSomeoneElse(message);
		} else {
			sendCommitResponseMessage(message, commitResultOptional.get());
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

	private void sendCommitResponseMessage(CommitMessage message, CommitResult commitResult) {
		CommitResponseMessage cm = new CommitResponseMessage(myServentInfo, message.getSender(), commitResult);
		cm.copyContextFrom(message);
		MessageUtil.sendAndForgetMessage(cm);
	}

	public Optional<RemoveResult> getViewFile(String resolvingConflictPath) {
		return pullFileForUs(resolvingConflictPath, Storage.LATEST_STORAGE_FILE_VERSION, PullType.VIEW);
	}

	// Chord maintenance

	private java.util.function.Consumer<Integer> leaveHandler;
	public void requestLeave(Consumer<Integer> lh) {
		leaveHandler = lh;
		LeaveRequestMessage lrm = new LeaveRequestMessage(myServentInfo, getSuccessorInfo(), getPredecessor());
		MessageUtil.sendAndForgetMessage(lrm);
	}

	public void handleLeave(ServentInfo leaveInitiator, ServentInfo sendersPredecessor) {
		setPredecessor(sendersPredecessor);
		SuccessorLeavingMessage slm = new SuccessorLeavingMessage(myServentInfo, getPredecessor(), leaveInitiator);
		MessageUtil.sendAndForgetMessage(slm);
	}

	public void handleSuccessorLeaving(ServentInfo leaveInitiator) {
		for(ServentInfo serventInfo: allNodeInfo) {
			System.out.println("" + serventInfo.getChordId());
		}
		allNodeInfo.remove(leaveInitiator);
		updateSuccessorTable();
		for(ServentInfo serventInfo: allNodeInfo) {
			System.out.println("" + serventInfo.getChordId());
		}

		LeaveGrantedMessage slm = new LeaveGrantedMessage(myServentInfo, leaveInitiator);
		MessageUtil.sendAndForgetMessage(slm);
		UpdateMessage update = new UpdateMessage(myServentInfo, getSuccessorInfo(), List.of(myServentInfo));
		MessageUtil.sendAndForgetMessage(update);
	}

	public void handleLeaveGranted() {
		ServentInitializer.notifyBootstrapAboutLeaving();
		leaveHandler.accept(5);
	}
}
