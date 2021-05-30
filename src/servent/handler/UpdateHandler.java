package servent.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.AppConfig;
import app.Logger;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private Message clientMessage;
	
	public UpdateHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			UpdateMessage message = (UpdateMessage) clientMessage;

			if (clientMessage.getSender().getNetworkLocation().equals(AppConfig.myServentInfo.getNetworkLocation()) == false) {
				//it was not us who initiated the update
				List<ServentInfo> newNodes = new ArrayList<>(message.getNodes());

				newNodes = newNodes.stream().filter(serventInfo -> {
					return serventInfo.getChordId() != AppConfig.myServentInfo.getChordId();
				}).collect(Collectors.toList());

				AppConfig.chordState.state.addNodes(newNodes, message.getRemovedNodes());

				newNodes.add(AppConfig.myServentInfo);

				UpdateMessage nextUpdate = new UpdateMessage(clientMessage.getSender(), AppConfig.chordState.state.getClosestSuccessor(), newNodes, message.getRemovedNodes());
				MessageUtil.sendAndForgetMessage(nextUpdate);

				ServentInfo updateInitiator = newNodes.get(0);
				//If initiator holds balancing lock (we just added it), release the lock so others (if any waiting) can also be added
				if(updateInitiator.getChordId() == AppConfig.chordState.state.getBalancingLockHoldingId()) {
					AppConfig.chordState.state.releaseBalancingLock();
				}
			} else {
				List<ServentInfo> newNodes = new ArrayList<>(message.getNodes());
				newNodes = newNodes.stream().filter(serventInfo -> {
					return serventInfo.getChordId() != AppConfig.myServentInfo.getChordId();
				}).collect(Collectors.toList());

				AppConfig.chordState.state.addNodes(newNodes, message.getRemovedNodes());
			}
		} else {
			Logger.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
