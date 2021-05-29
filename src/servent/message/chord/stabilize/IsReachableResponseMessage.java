package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class IsReachableResponseMessage extends TrackedMessage {

	private boolean isReachable;

	public IsReachableResponseMessage(ServentInfo sender, ServentInfo receiver, boolean isReachable) {
		super(MessageType.IS_REACHABLE_RESPONSE, sender, receiver, "");
		this.isReachable = isReachable;
	}

	public boolean isReachable() {
		return isReachable;
	}

	@Override
	public IsReachableResponseMessage newMessageFor(ServentInfo next) {
		IsReachableResponseMessage am = new IsReachableResponseMessage(getSender(), next, isReachable);
		am.copyContextFrom(this);
		return am;
	}
}
