package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.ResponseMessage;
import servent.message.TrackedMessage;

public class LockGrantedMessage extends ResponseMessage {

	private ServentInfo lockedNode;

	public LockGrantedMessage(ServentInfo sender, ServentInfo receiver, ServentInfo lockedNode) {
		super(MessageType.LOCK_GRANTED, sender, receiver, "");
		this.lockedNode = lockedNode;
	}

	public ServentInfo getLockInitiator() {
		return lockedNode;
	}

	@Override
	public LockGrantedMessage newMessageFor(ServentInfo next) {
		LockGrantedMessage am = new LockGrantedMessage(getSender(), next, lockedNode);
		am.copyContextFrom(this);
		return am;
	}
}
