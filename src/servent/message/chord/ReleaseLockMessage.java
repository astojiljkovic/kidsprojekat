package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class ReleaseLockMessage extends TrackedMessage {

	private ServentInfo lockInitiator;

	public ReleaseLockMessage(ServentInfo sender, ServentInfo receiver, ServentInfo lockInitiator) {
		super(MessageType.RELEASE_LOCK, sender, receiver, "");
		this.lockInitiator = lockInitiator;
	}

	public ServentInfo getLockInitiator() {
		return lockInitiator;
	}

	@Override
	public ReleaseLockMessage newMessageFor(ServentInfo next) {
		ReleaseLockMessage am = new ReleaseLockMessage(getSender(), next, lockInitiator);
		am.copyContextFrom(this);
		return am;
	}
}
