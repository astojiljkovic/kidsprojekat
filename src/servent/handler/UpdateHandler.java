package servent.handler;

import java.rmi.server.ServerCloneException;
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

//				ServentInfo newNodInfo = clientMessage.getSender();
//				List<ServentInfo> newNodes = new ArrayList<>();
//				newNodes.add(newNodInfo);

//				List<ServentInfo> newNodes = new ArrayList<>(message.getNodes());

//				newNodes.add(message.getSender());

				List<ServentInfo> newNodes = new ArrayList<>(message.getNodes());

				newNodes = newNodes.stream().filter(serventInfo -> {
					return serventInfo.getChordId() != AppConfig.myServentInfo.getChordId();
				}).collect(Collectors.toList());

				AppConfig.chordState.addNodes(newNodes);

				//Create ip:port:team
//				String currentNodeInfo = AppConfig.myServentInfo.getNetworkLocation().getIp() + ":" + String.valueOf(AppConfig.myServentInfo.getNetworkLocation().getPort()) + ":" + AppConfig.myServentInfo.getTeam();

//				String newMessageText = "";
//				if (clientMessage.getMessageText().equals("")) {
//					newMessageText = currentNodeInfo;
//				} else {
//					newMessageText = clientMessage.getMessageText() + "," + currentNodeInfo;
//				}

//				List<ServentInfo> nodes = message.getNodes();
				newNodes.add(AppConfig.myServentInfo);

				UpdateMessage nextUpdate = new UpdateMessage(clientMessage.getSender(), AppConfig.chordState.getSuccessorInfo(), newNodes);
				MessageUtil.sendAndForgetMessage(nextUpdate);
			} else {
//				String messageText = clientMessage.getMessageText(); //ip:port:team,ip:port:team,ip:port:team...
//				String[] ipPortTeams = messageText.split(",");
//
//				List<ServentInfo> allNodes = new ArrayList<>();
//				for (String ipPortTeam : ipPortTeams) {
//					String ip = ipPortTeam.split(":")[0];
//					String port = ipPortTeam.split(":")[1];
//					String team = ipPortTeam.split(":")[2];
//					allNodes.add(new ServentInfo(ip, Integer.parseInt(port), team));
//				}

				List<ServentInfo> newNodes = new ArrayList<>(message.getNodes());
				newNodes = newNodes.stream().filter(serventInfo -> {
					return serventInfo.getChordId() != AppConfig.myServentInfo.getChordId();
				}).collect(Collectors.toList());

				AppConfig.chordState.addNodes(newNodes);
			}
		} else {
			Logger.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
