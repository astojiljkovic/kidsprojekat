package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class RequestLockMessage extends TrackedMessage {

	private final ServentInfo initiator;
	private final ServentInfo lockTarget;
	private final boolean isNewNodeLock;

//	public RequestLockMessage(ServentInfo sender, ServentInfo receiver, ServentInfo initiator, ServentInfo lockTarget) {
//		super(MessageType.REQUEST_LOCK, sender, receiver, "");
//		this.initiator = initiator;
//		this.lockTarget = lockTarget;
//		this.isNewNodeLock = false;
//	}

	public RequestLockMessage(ServentInfo sender, ServentInfo receiver, ServentInfo initiator, ServentInfo lockTarget, boolean isNewNodeLock) {
		super(MessageType.REQUEST_LOCK, sender, receiver, "");
		this.initiator = initiator;
		this.lockTarget = lockTarget;
		this.isNewNodeLock = isNewNodeLock;
	}

	public ServentInfo getLockTarget() {
		return lockTarget;
	}

	public boolean isNewNodeLock() {
		return isNewNodeLock;
	}

	@Override
	protected String additionalContentToPrint() {
		return "<T:" + lockTarget + "|isNew:" + true + ">";
	}

	public ServentInfo getLockInitiator() {
		return initiator;
	}

	@Override
	public RequestLockMessage newMessageFor(ServentInfo next) {
		RequestLockMessage am = new RequestLockMessage(getSender(), next, initiator, lockTarget, isNewNodeLock);
		am.copyContextFrom(this);
		return am;
	}
}
