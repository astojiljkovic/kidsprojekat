package servent.message;

public class SorryMessage extends BasicMessage {

	private static final long serialVersionUID = 8866336621366084210L;

	public SorryMessage(String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort) {
		super(MessageType.SORRY, senderIp, senderPort, senderTeam, receiverIp, receiverPort);
	}
}
