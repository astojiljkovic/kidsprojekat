package servent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import app.*;
import app.storage.SillyGitStorageFile;
import servent.handler.*;
import servent.handler.chord.leave.LeaveGrantedHandler;
import servent.handler.chord.leave.LeaveRequestHandler;
import servent.handler.chord.leave.SuccLeavingHandler;
import servent.handler.data.AddHandler;
import servent.handler.data.CommitHandler;
import servent.handler.data.PullHandler;
import servent.handler.data.RemoveHandler;
import servent.message.Message;
import servent.message.ResponseMessage;
import servent.message.TrackedMessage;
import servent.message.chord.BusyMessage;
import servent.message.chord.LockGrantedMessage;
import servent.message.chord.ReleaseLockMessage;
import servent.message.chord.RequestLockMessage;
import servent.message.chord.stabilize.*;
import servent.message.data.RedundantCopyMessage;
import servent.message.util.MessageUtil;

import static app.AppConfig.myServentInfo;
import static servent.message.MessageType.UPDATE;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	public SimpleServentListener() {
		
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getNetworkLocation().getPort(), 100, InetAddress.getByName(AppConfig.myServentInfo.getNetworkLocation().getIp()));
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			Logger.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getNetworkLocation().getPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				Socket clientSocket = listenerSocket.accept();
				
				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);

				MessageHandler messageHandler = new NullHandler(clientMessage);

				if (clientMessage instanceof ResponseMessage) {
					ResponseMessage trackedMessage = (ResponseMessage) clientMessage;

					if(trackedMessage.getInitiator().equals(AppConfig.myServentInfo)) {
						Optional<ResponseMessageHandler> handler = MessageUtil.removeHandlerForId(trackedMessage.getInitialId());
						if(handler.isPresent()) {
							handler.get().setMessage(trackedMessage);
							messageHandler = handler.get();
						} else {
							Logger.timestampedStandardPrint("Handler picked message that was already timedOut " + clientMessage);
							continue;
						}
					} else { //Message is "Response", but I was not who initiated the question for response (Update)
						if (clientMessage.getMessageType() == UPDATE) {
							ResponseMessageHandler updateHandler = new UpdateHandler(null);
							updateHandler.setMessage((TrackedMessage) clientMessage);
							messageHandler = updateHandler;
						}
					}
				}
				else {

					/*
					 * Each message type has it's own handler.
					 * If we can get away with stateless handlers, we will,
					 * because that way is much simpler and less error prone.
					 */
					switch (clientMessage.getMessageType()) {
						case NEW_NODE:
							messageHandler = new NewNodeHandler(clientMessage);
							break;
						case WELCOME:
							messageHandler = new WelcomeHandler(clientMessage);
							break;
						case SORRY:
							messageHandler = new SorryHandler(clientMessage);
							break;
//						case UPDATE:
//							ResponseMessageHandler updateHandler = new UpdateHandler(null);
//							updateHandler.setMessage((TrackedMessage) clientMessage);
//							messageHandler = updateHandler;
//							break;
						case ADD:
							messageHandler = new AddHandler(clientMessage);
							break;
						case PULL:
							messageHandler = new PullHandler(clientMessage);
							break;
						case REMOVE:
							messageHandler = new RemoveHandler(clientMessage);
							break;
						case COMMIT:
							messageHandler = new CommitHandler(clientMessage);
							break;
						case LEAVE_REQUEST:
							messageHandler = new LeaveRequestHandler(clientMessage);
							break;
						case SUCC_LEAVING:
							messageHandler = new SuccLeavingHandler(clientMessage);
							break;
						case LEAVE_GRANTED:
							messageHandler = new LeaveGrantedHandler(clientMessage);
							break;
						case PING:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									PongMessage pm = new PongMessage(AppConfig.myServentInfo, clientMessage.getSender());
									pm.copyContextFrom((TrackedMessage) clientMessage);
									MessageUtil.sendAndForgetMessage(pm);
								}
							};
							break;
						case QUESTION_EXISTENCE:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									QuestionExistenceMessage qm = (QuestionExistenceMessage) clientMessage;
									if(qm.getReceiver().equals(AppConfig.myServentInfo)) {

										ServentInfo softDeadNode = qm.getSoftDeadNode();
										PingMessage ping = new PingMessage(AppConfig.myServentInfo, softDeadNode);
										MessageUtil.sendTrackedMessageAwaitingResponse(ping, new ResponseMessageHandler() {
													@Override
													public void run() {
														QuestionExistenceResponseMessage response = new QuestionExistenceResponseMessage(AppConfig.myServentInfo, qm.getSender(), false, softDeadNode);
														response.copyContextFrom(qm);
														MessageUtil.sendAndForgetMessage(response);
													}
												}, 10000,
												invocation -> { //timeout
													QuestionExistenceResponseMessage response = new QuestionExistenceResponseMessage(AppConfig.myServentInfo, qm.getSender(), true, softDeadNode);
													response.copyContextFrom(qm);
													MessageUtil.sendAndForgetMessage(response);
													return -1;
												});
									} else {
										ServentInfo nextNode = AppConfig.chordState.state.getNextNodeForKey(qm.getReceiver().getChordId());
										QuestionExistenceMessage forwarded = new QuestionExistenceMessage(qm.getSender(), nextNode, qm.getSoftDeadNode());
										forwarded.copyContextFrom(qm);
										MessageUtil.sendAndForgetMessage(forwarded);
									}
								}
							};
							break;
						case NEW_PREDECESSOR:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									NewPredecessorMessage npm = (NewPredecessorMessage) clientMessage;
									if(npm.getReceiver().equals(AppConfig.myServentInfo)) {
										ServentInfo currentPred = AppConfig.chordState.state.getPredecessor();

										AppConfig.chordState.state.addNodes(Collections.emptyList(), List.of(currentPred));
	//									AppConfig.chordState.state.setPredecessor(npm.getNewPredecessor());

										//TODO: don't return null successors
										List<ServentInfo> mySuccs = new ArrayList<>(Arrays.asList(AppConfig.chordState.state.getSuccessors()));

										mySuccs.add(0, AppConfig.myServentInfo);

										if (mySuccs.size() > ChordState.State.MAX_SUCCESSORS) {
											mySuccs.remove(mySuccs.size() - 1);
										}

										List<ServentInfo> succis = mySuccs.stream().filter(Objects::nonNull).collect(Collectors.toList());
										NewPredecessorResponseMessage response = new NewPredecessorResponseMessage(AppConfig.myServentInfo, clientMessage.getSender(),
												succis);
										response.copyContextFrom(npm);
										MessageUtil.sendAndForgetMessage(response);
									} else {
										ServentInfo nextNode = AppConfig.chordState.state.getNextNodeForKey(npm.getReceiver().getChordId());
										NewPredecessorMessage forwarded = new NewPredecessorMessage(npm.getSender(), nextNode, npm.getNewPredecessor());
										forwarded.copyContextFrom(npm);
										MessageUtil.sendAndForgetMessage(forwarded);
									}
								}
							};
							break;
						case REDUNDANT_COPY:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									RedundantCopyMessage npm = (RedundantCopyMessage) clientMessage;
									if(npm.getReceiver().equals(AppConfig.myServentInfo)) {
										System.out.println("received files for replication");
										for(SillyGitStorageFile file: npm.getData()) {
											System.out.println("" + file);
										}
										AppConfig.chordState.storeReplicaData(npm.getMainNode().getChordId(), npm.getData());
									} else {
										ServentInfo nextNode = AppConfig.chordState.state.getNextNodeForKey(npm.getReceiver().getChordId());
										RedundantCopyMessage forwarded = new RedundantCopyMessage(npm.getSender(), nextNode, npm.getMainNode(), npm.getReplicationTarget(), npm.getData());
										forwarded.copyContextFrom(npm);
										MessageUtil.sendAndForgetMessage(forwarded);
									}
								}
							};
							break;
						case RELEASE_LOCK:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									ReleaseLockMessage rlm = (ReleaseLockMessage) clientMessage;
									if(AppConfig.chordState.state.isKeyMine(rlm.getReceiver().getChordId())) {
										AppConfig.chordState.state.releaseBalancingLock(rlm.getLockInitiator().getChordId());
										if (rlm.isStopForwarding()) {
											System.out.println("stop forwarding");
											AppConfig.chordState.state.stopNodeForwarding();
										}
									} else {
										ReleaseLockMessage rlToForward = rlm.newMessageFor(AppConfig.chordState.state.getNextNodeForKey(rlm.getReceiver().getChordId()));
										MessageUtil.sendAndForgetMessage(rlToForward);
									}
								}
							};
							break;
						case REQUEST_LOCK:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									RequestLockMessage message = (RequestLockMessage) clientMessage;
//									ServentInfo lockReceiver;
									if(message.isNewNodeLock()) {
										//if my predecessor
										if (AppConfig.chordState.state.isKeyMine(message.getSender().getChordId())) {
											//handle (base on sender)
											handleMyself(message, true);
										} else {
											//forward (based on sender.choardId)
											forward(message, true);
										}
									} else {
										if (message.getLockTarget().equals(myServentInfo)) {
											//handle (based on lock target)
											handleMyself(message, false);
										} else {
											//forward (based on lock target)
											forward(message, false);
										}
									}
								}

								private void forward(RequestLockMessage message, boolean basedOnSender) {
									ServentInfo nextNode;
									if (basedOnSender) {
										nextNode = AppConfig.chordState.state.getNextNodeForKey(message.getSender().getChordId());
									} else {
										nextNode = AppConfig.chordState.state.getNextNodeForKey(message.getLockTarget().getChordId());
									}
									RequestLockMessage rlmToForward = new RequestLockMessage(message.getSender(), nextNode, message.getLockInitiator(), message.getLockTarget(), message.isNewNodeLock());
									rlmToForward.copyContextFrom(message);
									MessageUtil.sendAndForgetMessage(rlmToForward);
								}

								private void handleMyself(RequestLockMessage message, boolean basedOnSender) {
									ServentInfo node;
									if (basedOnSender) {
										node = message.getSender();
									} else {
										node = message.getLockTarget();
									}
									if (AppConfig.chordState.state.acquireBalancingLock(node.getChordId())) {
										LockGrantedMessage lgm = new LockGrantedMessage(AppConfig.myServentInfo, message.getSender(), AppConfig.myServentInfo);
										lgm.copyContextFrom(message);
										MessageUtil.sendAndForgetMessage(lgm);
									} else {
										BusyMessage bm = new BusyMessage(myServentInfo, message.getSender());
										bm.copyContextFrom(message);
										MessageUtil.sendAndForgetMessage(bm);
									}
								}
							};
							break;
						case POISON:
							break;
					}
				}

				threadPool.execute(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				Logger.timestampedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Logger.timestampedStandardPrint("Simple Servent listener stopped");
	}

	@Override
	public void stop() {
		this.working = false;

	}

}
