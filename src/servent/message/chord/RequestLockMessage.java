package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class RequestLockMessage extends TrackedMessage {

	private ServentInfo initiator;
	private ServentInfo lockTarget;

	public RequestLockMessage(ServentInfo sender, ServentInfo receiver, ServentInfo initiator, ServentInfo lockTarget) {
		super(MessageType.REQUEST_LOCK, sender, receiver, "");
		this.initiator = initiator;
		this.lockTarget = lockTarget;
	}

	public ServentInfo getLockTarget() {
		return lockTarget;
	}

	public ServentInfo getLockInitiator() {
		return initiator;
	}

	@Override
	public RequestLockMessage newMessageFor(ServentInfo next) {
		RequestLockMessage am = new RequestLockMessage(getSender(), next, initiator, lockTarget);
		am.copyContextFrom(this);
		return am;
	}
}
