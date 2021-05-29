package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.SendAndForgetMessage;
import servent.message.TrackedMessage;
import servent.message.data.AddMessage;

public class PingMessage extends TrackedMessage {

	public PingMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.PING, sender, receiver, "");
	}

	@Override
	public PingMessage newMessageFor(ServentInfo next) {
		PingMessage am = new PingMessage(getSender(), next);
		am.copyContextFrom(this);
		return am;
	}
}
