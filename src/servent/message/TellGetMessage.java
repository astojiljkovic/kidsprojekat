package servent.message;

public class TellGetMessage extends BasicMessage {

	private static final long serialVersionUID = -6213394344524749872L;

	public TellGetMessage(String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort, int key, int value) {
		super(MessageType.TELL_GET, senderIp, senderPort, senderTeam, receiverIp, receiverPort, key + ":" + value);
	}
}
