package servent.message;

import app.ServentInfo;

public class NewNodeMessage extends BasicMessage {

	private static final long serialVersionUID = 3899837286642127636L;

	public NewNodeMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.NEW_NODE, sender, receiver);
	}

	public NewNodeMessage(ServentInfo sender, String receiverIp, int receiverPort) {
		this(sender, new ServentInfo(receiverIp, receiverPort, "UNKNOWN"));
	}
}
