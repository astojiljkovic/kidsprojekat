package servent.message;

public class UpdateMessage extends BasicMessage {

	private static final long serialVersionUID = 3586102505319194978L;

	public UpdateMessage(String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort, String text) {
		super(MessageType.UPDATE, senderIp, senderPort, senderTeam, receiverIp, receiverPort, text);
	}
}
