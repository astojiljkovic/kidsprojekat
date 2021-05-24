package servent.message;

public class AskGetMessage extends BasicMessage {

	private static final long serialVersionUID = -8558031124520315033L;

	public AskGetMessage(String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort, String text) {
		super(MessageType.ASK_GET, senderIp, senderPort, senderTeam, receiverIp, receiverPort, text);
	}
}
