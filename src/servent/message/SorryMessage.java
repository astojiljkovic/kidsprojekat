package servent.message;

import app.ServentInfo;

public class SorryMessage extends SendAndForgetMessage {

	private static final long serialVersionUID = 8866336621366084210L;

	public SorryMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.SORRY, sender, receiver);
	}
}
