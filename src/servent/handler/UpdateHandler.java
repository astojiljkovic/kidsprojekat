package servent.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.AppConfig;
import app.ServentInfo;
import servent.message.UpdateMessage;
import servent.message.chord.ReleaseLockMessage;
import servent.message.util.MessageUtil;

public class UpdateHandler extends ResponseMessageHandler {

//	private Message clientMessage;
	private ServentInfo unlockRemoteServent;

	public UpdateHandler(ServentInfo unlockRemoteServent) {
//		this.clientMessage = clientMessage;
		this.unlockRemoteServent = unlockRemoteServent;
	}
	
	@Override
	public void run() {
//		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			UpdateMessage updateMessage = (UpdateMessage) message;

			if (updateMessage.getSender().getNetworkLocation().equals(AppConfig.myServentInfo.getNetworkLocation()) == false) {
				//it was not us who initiated the update
				List<ServentInfo> newNodes = new ArrayList<>(updateMessage.getNodes());

				newNodes = newNodes.stream().filter(serventInfo -> {
					return serventInfo.getChordId() != AppConfig.myServentInfo.getChordId();
				}).collect(Collectors.toList());

				AppConfig.chordState.state.addNodes(newNodes, updateMessage.getRemovedNodes());

				newNodes.add(AppConfig.myServentInfo);

				UpdateMessage nextUpdate = new UpdateMessage(updateMessage.getSender(), AppConfig.chordState.state.getClosestSuccessor(), newNodes, updateMessage.getRemovedNodes());
				nextUpdate.copyContextFrom(message);
				MessageUtil.sendAndForgetMessage(nextUpdate);
			} else {
				List<ServentInfo> newNodes = new ArrayList<>(updateMessage.getNodes());
				newNodes = newNodes.stream().filter(serventInfo -> {
					return serventInfo.getChordId() != AppConfig.myServentInfo.getChordId();
				}).collect(Collectors.toList());

				AppConfig.chordState.state.addNodes(newNodes, updateMessage.getRemovedNodes());

				//Is this welcome update we sent???!?!?! //TODO: FIX
//				ServentInfo updateInitiator = newNodes.get(0);
//				//If initiator holds balancing lock (we just added it), release the lock so others (if any waiting) can also be added
//				if(updateInitiator.getChordId() == AppConfig.chordState.state.getBalancingLockHoldingId()) {
//					AppConfig.chordState.state.releaseBalancingLock();
//				}
				if (unlockRemoteServent != null) {
//					try {
//						Thread.sleep(10000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
					//Tell my succ to release lock and stop forwarding messages to me
					ReleaseLockMessage rlm = new ReleaseLockMessage(AppConfig.myServentInfo, unlockRemoteServent, AppConfig.myServentInfo, true);
					MessageUtil.sendAndForgetMessage(rlm);
				}
			}
//		} else {
//			Logger.timestampedErrorPrint("Update message handler got message that is not UPDATE");
//		}
	}

}
