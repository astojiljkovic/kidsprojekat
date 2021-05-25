package servent.message;

import app.ServentInfo;

public class AskGetMessage extends BasicMessage {

	private static final long serialVersionUID = -8558031124520315033L;

	public AskGetMessage(ServentInfo sender, ServentInfo receiver, String text) {
		super(MessageType.ASK_GET, sender, receiver, text);
	}

//	public AskGetMessage(String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort, String text) {
//		super(MessageType.ASK_GET, senderIp, senderPort, senderTeam, receiverIp, receiverPort, text);
//	}
}
