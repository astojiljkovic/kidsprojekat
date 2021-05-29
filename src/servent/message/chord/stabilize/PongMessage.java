package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.ResponseMessage;
import servent.message.TrackedMessage;

public class PongMessage extends ResponseMessage {

	public PongMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.PONG, sender, receiver, "");
	}

	@Override
	public PongMessage newMessageFor(ServentInfo next) {
		PongMessage am = new PongMessage(getSender(), next);
		am.copyContextFrom(this);
		return am;
	}
}
