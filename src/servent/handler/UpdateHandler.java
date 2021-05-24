package servent.handler;

import java.util.ArrayList;
import java.util.List;

import app.AppConfig;
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
			if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				ServentInfo newNodInfo = new ServentInfo(clientMessage.getSenderIp(), clientMessage.getSenderPort(), clientMessage.getSenderTeam());
				List<ServentInfo> newNodes = new ArrayList<>();
				newNodes.add(newNodInfo);
				
				AppConfig.chordState.addNodes(newNodes);

				//Create ip:port:team
				String currentNodeInfo = AppConfig.myServentInfo.getIpAddress() + ":" + String.valueOf(AppConfig.myServentInfo.getListenerPort()) + ":" + AppConfig.myServentInfo.getTeam();

				String newMessageText = "";
				if (clientMessage.getMessageText().equals("")) {
					newMessageText = currentNodeInfo;
				} else {
					newMessageText = clientMessage.getMessageText() + "," + currentNodeInfo;
				}
				Message nextUpdate = new UpdateMessage(clientMessage.getSenderIp(), clientMessage.getSenderPort(), clientMessage.getSenderTeam(), AppConfig.chordState.getNextNodeIp(), AppConfig.chordState.getNextNodePort(),
						newMessageText);
				MessageUtil.sendMessage(nextUpdate);
			} else {
				String messageText = clientMessage.getMessageText(); //ip:port:team,ip:port:team,ip:port:team...
				String[] ipPortTeams = messageText.split(",");
				
				List<ServentInfo> allNodes = new ArrayList<>();
				for (String ipPortTeam : ipPortTeams) {
					String ip = ipPortTeam.split(":")[0];
					String port = ipPortTeam.split(":")[1];
					String team = ipPortTeam.split(":")[2];
					allNodes.add(new ServentInfo(ip, Integer.parseInt(port), team));
				}
				AppConfig.chordState.addNodes(allNodes);
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
