package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class ReleaseLockMessage extends TrackedMessage {

	private ServentInfo lockInitiator;
	private boolean stopForwarding;

	public ReleaseLockMessage(ServentInfo sender, ServentInfo receiver, ServentInfo lockInitiator, boolean stopForwarding) {
		super(MessageType.RELEASE_LOCK, sender, receiver, "");
		this.lockInitiator = lockInitiator;
		this.stopForwarding = stopForwarding;
	}

	public ServentInfo getLockInitiator() {
		return lockInitiator;
	}

	public boolean isStopForwarding() {
		return stopForwarding;
	}

	@Override
	public ReleaseLockMessage newMessageFor(ServentInfo next) {
		ReleaseLockMessage am = new ReleaseLockMessage(getSender(), next, lockInitiator, stopForwarding);
		am.copyContextFrom(this);
		return am;
	}
}
