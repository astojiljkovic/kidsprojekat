package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class IsReachableMessage extends TrackedMessage {

	public IsReachableMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.IS_REACHABLE, sender, receiver, "");
	}

	@Override
	public IsReachableMessage newMessageFor(ServentInfo next) {
		IsReachableMessage am = new IsReachableMessage(getSender(), next);
		am.copyContextFrom(this);
		return am;
	}
}
