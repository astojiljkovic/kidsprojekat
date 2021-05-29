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

import app.chord.StateStabilizer;
import app.chord.SuspiciousNode;
import app.git.add.AddResult;
import app.git.commit.CommitResult;
import app.git.pull.RemoveResult;
import app.storage.CommitConflictStorageException;
import app.storage.FileAlreadyAddedStorageException;
import app.storage.FileDoesntExistStorageException;
import servent.handler.ResponseMessageHandler;
import servent.handler.data.AddResponseHandler;
import servent.handler.data.CommitResponseHandler;
import servent.handler.data.PullResponseHandler;
import servent.handler.data.RemoveResponseHandler;
import servent.message.*;
import servent.message.chord.leave.LeaveGrantedMessage;
import servent.message.chord.leave.LeaveRequestMessage;
import servent.message.chord.leave.SuccessorLeavingMessage;
import servent.message.chord.stabilize.NewPredecessorMessage;
import servent.message.chord.stabilize.NewPredecessorResponseMessage;
import servent.message.chord.stabilize.QuestionExistenceMessage;
import servent.message.chord.stabilize.QuestionExistenceResponseMessage;
import servent.message.data.*;
import servent.message.util.MessageUtil;

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

	public static class State {

		public static final int MAX_SUCCESSORS = 3;
		private ServentInfo[] successors = new ServentInfo[MAX_SUCCESSORS];

		private Set<SuspiciousNode> suspiciousNodes = new HashSet<>();

		private ServentInfo[] fingerTable;

		private ServentInfo predecessorInfo;

		private final Set<ServentInfo> allNodeInfo = new HashSet<>();

//		private final int chordLevel; //log_2(CHORD_SIZE)

		private final StateStabilizer stateStabilizer;

		private State() {
			int tmpChordLvl = 1;
			int tmp = CHORD_SIZE;
			while (tmp != 2) {
				if (tmp % 2 != 0) { //not a power of 2
					throw new NumberFormatException();
				}
				tmp /= 2;
				tmpChordLvl++;
			}
//			this.chordLevel = tmpChordLvl;

			fingerTable = new ServentInfo[tmpChordLvl];
			Arrays.fill(fingerTable, null);

			predecessorInfo = null;

			stateStabilizer = new StateStabilizer(
					nodeInfo -> { //node answered
						removeSuspiciousNode(nodeInfo);
					},
					(node, isSoftTimeout) -> { //node didn't answer
						System.out.println("The node is slacking " + node + "is soft? " + isSoftTimeout);

						addSuspiciousNode(new SuspiciousNode(node, isSoftTimeout ? SuspiciousNode.State.SOFT_DEAD : SuspiciousNode.State.DEAD));
					});
		}

		private void removeSuspiciousNode(ServentInfo nodeInfo) {
			suspiciousNodes.removeIf(suspiciousNode -> suspiciousNode.getServentInfo().equals(nodeInfo));
		}

		private void addSuspiciousNode(SuspiciousNode node) {
			suspiciousNodes.removeIf(suspiciousNode -> suspiciousNode.getServentInfo().equals(node.getServentInfo()));

			suspiciousNodes.add(node);

			if (node.getState() == SuspiciousNode.State.SOFT_DEAD && node.getServentInfo().equals(getClosestSuccessor())) { //start questioning his existence
				//get successor of questionable node
				ServentInfo succiOfSucci = getSucciOfFailingSucci(node.getServentInfo()); //// TODO: 29.5.21. Check if obtained succ is already in our suspicious table
				if (succiOfSucci == node.getServentInfo()) { //we don't have successor of troubled node - edge case
					Logger.timestampedErrorPrint("No successor of failed node, can't recover " + node.getServentInfo()); //TODO: fix if possible (going back through predecessors)
					return;
				}
				QuestionExistenceMessage question = new QuestionExistenceMessage(myServentInfo, succiOfSucci, node.getServentInfo());

				MessageUtil.sendTrackedMessageAwaitingResponse(question, new ResponseMessageHandler() {
					@Override
					public void run() {
						QuestionExistenceResponseMessage msg = (QuestionExistenceResponseMessage) message;
						handleSuspiciousNodeUpdate(msg.getNode(), msg.isDead());
					}
				});
			}
		}

		private ServentInfo getSucciOfFailingSucci(ServentInfo failingSucc) {
			boolean isNextValid = false;
			for(ServentInfo succ: getSuccessors()) {
				if(succ != null && succ.getChordId() == failingSucc.getChordId()) {
					isNextValid = true;
				} else if(succ != null && isNextValid) {
					return succ;
				}
			}
			Logger.timestampedErrorPrint("No successor of failed node, can't recover " + failingSucc); //TODO: fix if possible (going back through predecessors)
			return failingSucc; //This is not true
		}

		private void handleSuspiciousNodeUpdate(ServentInfo serventInfo, boolean didDie) {
			Optional<SuspiciousNode> sn = suspiciousNodes.stream().filter(suspiciousNode -> suspiciousNode.getServentInfo().equals(serventInfo)).findFirst();
			if(sn.isEmpty()) { //node not suspicious anymore (ping came in the meantime)
				return;
			}
			if (didDie && sn.get().getState() == SuspiciousNode.State.DEAD) { //start recovery

				NewPredecessorMessage newPredMessage = new NewPredecessorMessage(myServentInfo, getSucciOfFailingSucci(serventInfo), myServentInfo);
				MessageUtil.sendTrackedMessageAwaitingResponse(newPredMessage, new ResponseMessageHandler() {
					@Override
					public void run() {
						NewPredecessorResponseMessage response = (NewPredecessorResponseMessage) message;

						setSuccessors(response.getSuccessors());
						allNodeInfo.removeIf(everyNode -> everyNode.getChordId() == sn.get().getServentInfo().getChordId());
						updateFingerTable();
						UpdateMessage um = new UpdateMessage(myServentInfo, getClosestSuccessor(), List.of(myServentInfo), List.of(sn.get().getServentInfo()));
						MessageUtil.sendAndForgetMessage(um);
					}
				});
			} else if (didDie && sn.get().getState() == SuspiciousNode.State.SOFT_DEAD) { //We just wait for either all good or DEAD from our stabilizer
				//do nothing
			} else if (!didDie) { //Someone else returned that node is ok
				suspiciousNodes.removeIf(suspiciousNode -> suspiciousNode.getServentInfo().equals(serventInfo));
			}
		}
		/**
		 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
		 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
		 *
		 */
		public void addNodes(List<ServentInfo> newNodes, List<ServentInfo> removedNodes) {
			allNodeInfo.addAll(newNodes);
			allNodeInfo.removeAll(removedNodes);

			updateSuccessors();
			fingerTable[0] = successors[0]; //TODO: fix generation of finger table
			updatePredecessor();
			updateFingerTable();
		}

		private void updateSuccessors() {
			int myId = myServentInfo.getChordId();

			//me 32
			//bigger 48 56 67
			//smaller 1 2 7
//			List<ServentInfo> bigger = allNodeInfo.stream().filter(serventInfo -> serventInfo.getChordId() >= myId).sorted(Comparator.comparingInt(ServentInfo::getChordId)).collect(Collectors.toList());
//			List<ServentInfo> smaller = allNodeInfo.stream().filter(serventInfo -> serventInfo.getChordId() < myId).sorted(Comparator.comparingInt(ServentInfo::getChordId)).collect(Collectors.toList());
//
//			List<ServentInfo> orderedSuccis = new ArrayList<>();
//			orderedSuccis.addAll(bigger);
//			orderedSuccis.addAll(smaller);
//
//			System.out.println("---Updating succies in update succs---");
//			orderedSuccis.forEach(serventInfo -> {
//				System.out.println("" + serventInfo);
//			});

			setSuccessors(new ArrayList<>(allNodeInfo));
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
				while (fingerTable[skip].getChordId() > fingerTable[startInd].getChordId()) {
					startInd++;
					skip++;
				}
			}

			int previousId = fingerTable[startInd].getChordId();

			for (int i = startInd + 1; i < fingerTable.length; i++) {
				if (fingerTable[i] == null) {
					Logger.timestampedErrorPrint("Couldn't find successor for " + key);
					break;
				}

				int successorId = fingerTable[i].getChordId();

				if (successorId >= key) {
					return fingerTable[i-1];
				}
				if (key > previousId && successorId < previousId) { //overflow
					return fingerTable[i-1];
				}
				previousId = successorId;
			}
			//if we have only one node in all slots in the table, we might get here
			//then we can return any item
			return fingerTable[0];
		}

		public ServentInfo[] getFingerTable() {
			return fingerTable;
		}

		public ServentInfo[] getSuccessorInfoForPrint() {
			return successors;
		}

		public ServentInfo getClosestSuccessor() {
			return getSuccessor(0);
		}

		public ServentInfo getSuccessor(int desiredSucc) { //0 closest - 2 furthest
			for(int i = successors.length - 1; i > 0; i--) {
				if (i == desiredSucc && successors[i] != null) {
					return successors[i];
				}
				if (desiredSucc > i && successors[i] != null) {
					return successors[i];
				}
			}

			return successors[0]; //this shouldn't be reached
		}

		public ServentInfo[] getSuccessors() { return successors; }

		public ServentInfo getPredecessor() {
			return predecessorInfo;
		}

		public void setPredecessor(ServentInfo newNodeInfo) {
			this.predecessorInfo = newNodeInfo;
		}

		public void setSuccessors(List<ServentInfo> receivedSuccessors) {
			System.out.println("Received Succis");
			for(ServentInfo si: receivedSuccessors) {
				System.out.println("" + si);
			}

			int myId = myServentInfo.getChordId();

			List<ServentInfo> bigger = receivedSuccessors.stream().filter(serventInfo -> serventInfo.getChordId() >= myId).sorted(Comparator.comparingInt(ServentInfo::getChordId)).collect(Collectors.toList());
			List<ServentInfo> smaller = receivedSuccessors.stream().filter(serventInfo -> serventInfo.getChordId() < myId).sorted(Comparator.comparingInt(ServentInfo::getChordId)).collect(Collectors.toList());

			List<ServentInfo> orderedSuccis = new ArrayList<>();
			orderedSuccis.addAll(bigger);
			orderedSuccis.addAll(smaller);

			List<ServentInfo> sortedSuccis = orderedSuccis.stream()
					.filter(Objects::nonNull)
					.filter(serventInfo -> !serventInfo.equals(myServentInfo)) // in case when our successor has us as one if his successors
//					.sorted(Comparator.comparingInt(ServentInfo::getChordId))
					.collect(Collectors.toList());
			for(int i = 0; i < sortedSuccis.size(); i++) {
				this.successors[i] = sortedSuccis.get(i);
			}

			List<ServentInfo> succisToPing = Arrays.stream(successors).filter(Objects::nonNull).collect(Collectors.toList());

			stateStabilizer.pingNodes(succisToPing);

			System.out.println("New Succis");
			for(ServentInfo si: receivedSuccessors) {
				System.out.println("" + si);
			}

			fingerTable[0] = successors[0]; //TODO: fix
//			updateFingerTable();
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

		private void updateFingerTable() {
			//first node after me has to be successorTable[0]

			List<Integer> values = new ArrayList<>();
			for(int i = 0; i < fingerTable.length; i++) {
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

			for(int i = 0; i < fingerTable.length; i++) {
				fingerTable[i] = succis.get(i);
			}

			System.out.println("New succ table" );

			for(int i = 0; i < fingerTable.length; i++) {
				System.out.println("" + (((int) (myServentInfo.getChordId() + Math.pow(2, i))) % CHORD_SIZE) + " " + fingerTable[i]);
			}
//		for(ServentInfo info: successorTable) {
//
//			System.out.println("" + info);
//		}
		}

		private void updatePredecessor() {
			Set<ServentInfo> biggerIdNodes = new HashSet<>();
			Set<ServentInfo> smallerIdNodes = new HashSet<>();

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
				predecessorInfo = smallerIdNodes.stream().max(Comparator.comparingInt(ServentInfo::getChordId)).get();// smallerIdNodes.get(smallerIdNodes.size()-1);
			} else {
				predecessorInfo = biggerIdNodes.stream().max(Comparator.comparingInt(ServentInfo::getChordId)).get(); //.get(biggerIdNodes.size()-1);
			}
			System.out.println("predecessor " + predecessorInfo);

			for(ServentInfo succ: successors) {
				System.out.println("succi " + succ);
			}
		}
	}

	public State state = new State();

	public static int CHORD_SIZE;
	public static int chordHash(int value) {
		System.out.println("Hash " + value);
		int absValue = Math.abs(value) % 50000; // TODO: 25.5.21. Fix dirty cheat for positive values
		return 61 * absValue % CHORD_SIZE;
	}

	public static int hashForFilePath(String pathInDir) {
		try {
			byte []shaBytes = MessageDigest.getInstance("SHA-1").digest(Path.of(pathInDir).getName(0).toString().getBytes());
			ByteBuffer wrapped = ByteBuffer.wrap(shaBytes);
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
		state.setSuccessors(welcomeMsg.getSuccessors());

		storage.addTransferedFiles(welcomeMsg.getFiles());
		
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

		if (state.isKeyMine(key)) {
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

		ServentInfo nextNode = state.getNextNodeForKey(key);

		MessageUtil.sendAndForgetMessage(message.newMessageFor(nextNode));
	}

	private void sendAddFilesForMe(List<SillyGitFile> sillyGitFiles) {
		int key = hashForFilePath(sillyGitFiles.get(0).getPathInWorkDir());

		ServentInfo nextNode = state.getNextNodeForKey(key);

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

		if (state.isKeyMine(key)) {
			Storage.GetResult storageResult = storage.get(fileName, version);
			return Optional.of(new RemoveResult(storageResult.getFailedPaths(), storageResult.getSuccesses()));
		}

		return Optional.empty();
	}

	private void forwardPullMessage(PullMessage originalMessage) {
		int key = hashForFilePath(originalMessage.getFileName());
		ServentInfo nextNode = state.getNextNodeForKey(key);

		PullMessage messageToForward = originalMessage.newMessageFor(nextNode);
		MessageUtil.sendAndForgetMessage(messageToForward);
	}

	private void sendPullMessageForMe(String filePath, int version, PullType pullType) {
		int key = hashForFilePath(filePath);
		ServentInfo nextNode = state.getNextNodeForKey(key);

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

		if (state.isKeyMine(key)) {
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

		ServentInfo nextNode = state.getNextNodeForKey(key);
		RemoveMessage rm = new RemoveMessage(myServentInfo, nextNode, removePath);

		MessageUtil.sendTrackedMessageAwaitingResponse(rm, new RemoveResponseHandler());
	}

	private void forwardRemoveMessageForSomeoneElse(RemoveMessage originalMessage) {
		int key = hashForFilePath(originalMessage.getRemovePath());

		ServentInfo nextNode = state.getNextNodeForKey(key);

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

		if (state.isKeyMine(key)) {
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

		ServentInfo nextNode = state.getNextNodeForKey(key);
		CommitMessage cm = new CommitMessage(myServentInfo, nextNode, filesInWorkDir, isCommitResolution);

		MessageUtil.sendTrackedMessageAwaitingResponse(cm, new CommitResponseHandler(isCommitResolution));
	}

	private void forwardCommitMessageForSomeoneElse(CommitMessage originalMessage) {
		SillyGitFile referenceFile = originalMessage.getFilesToCommit().get(0);
		int key = hashForFilePath(referenceFile.getPathInWorkDir());

		ServentInfo nextNode = state.getNextNodeForKey(key);

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

	// Leave

	private java.util.function.Consumer<Integer> leaveHandler;
	public void requestLeave(Consumer<Integer> lh) {
		leaveHandler = lh;
		List<String> allFileNames = storage.getAllStoredUnversionedFileNamesRelativeToStorageRoot();
		List<SillyGitStorageFile> data = storage.removeFilesOnRelativePathsReturningGitFiles(allFileNames);
		LeaveRequestMessage lrm = new LeaveRequestMessage(myServentInfo, state.getClosestSuccessor(), state.getPredecessor(), data);
		MessageUtil.sendAndForgetMessage(lrm);
	}

	public void handleLeave(LeaveRequestMessage leaveRequestMessage) { //ServentInfo leaveInitiator, ServentInfo sendersPredecessor) {
		ServentInfo leaveInitiator = leaveRequestMessage.getSender();
		removeNodeFromAllNodes(leaveInitiator);
		state.setPredecessor(leaveRequestMessage.getPredecessor());
		storage.addTransferedFiles(leaveRequestMessage.getData());

		SuccessorLeavingMessage slm = new SuccessorLeavingMessage(myServentInfo, state.getPredecessor(), leaveInitiator);
		MessageUtil.sendAndForgetMessage(slm);
	}

	private void removeNodeFromAllNodes(ServentInfo node) {
		state.allNodeInfo.remove(node);
		state.updatePredecessor();
		state.updateFingerTable();
	}

	public void handleSuccessorLeaving(ServentInfo leaveInitiator) {
		removeNodeFromAllNodes(leaveInitiator);
		LeaveGrantedMessage slm = new LeaveGrantedMessage(myServentInfo, leaveInitiator);
		MessageUtil.sendAndForgetMessage(slm);
		UpdateMessage update = new UpdateMessage(myServentInfo, state.getClosestSuccessor(), List.of(myServentInfo), List.of(leaveInitiator));
		MessageUtil.sendAndForgetMessage(update);
	}

	public void handleLeaveGranted() {
		ServentInitializer.notifyBootstrapAboutLeaving();
		leaveHandler.accept(5);
	}

	//  Stabilize
}
