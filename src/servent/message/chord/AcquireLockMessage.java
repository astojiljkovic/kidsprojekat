package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class AcquireLockMessage extends TrackedMessage {

	public AcquireLockMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.ACQUIRE_LOCK, sender, receiver, "");
	}

	@Override
	public AcquireLockMessage newMessageFor(ServentInfo next) {
		AcquireLockMessage am = new AcquireLockMessage(getSender(), next);
		am.copyContextFrom(this);
		return am;
	}
}
