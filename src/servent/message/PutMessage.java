package servent.message;

public class PutMessage extends BasicMessage {

	private static final long serialVersionUID = 5163039209888734276L;

	public PutMessage(String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort, int key, int value) {
		super(MessageType.PUT, senderIp, senderPort, senderTeam, receiverIp, receiverPort, key + ":" + value);
	}
}
