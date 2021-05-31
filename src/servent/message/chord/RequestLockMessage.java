package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class RequestLockMessage extends TrackedMessage {

	private ServentInfo initiator;

	public RequestLockMessage(ServentInfo sender, ServentInfo receiver, ServentInfo initiator) {
		super(MessageType.REQUEST_LOCK, sender, receiver, "");
		this.initiator = initiator;
	}

	public ServentInfo getLockInitiator() {
		return initiator;
	}

	@Override
	public RequestLockMessage newMessageFor(ServentInfo next) {
		RequestLockMessage am = new RequestLockMessage(getSender(), next, initiator);
		am.copyContextFrom(this);
		return am;
	}
}
