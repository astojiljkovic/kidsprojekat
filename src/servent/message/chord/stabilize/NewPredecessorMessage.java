package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class NewPredecessorMessage extends TrackedMessage {

	private ServentInfo newPredecessor;

	public NewPredecessorMessage(ServentInfo sender, ServentInfo receiver, ServentInfo newPredecessor) {
		super(MessageType.NEW_PREDECESSOR, sender, receiver, "");
		this.newPredecessor = newPredecessor;
	}

	public ServentInfo getNewPredecessor() {
		return newPredecessor;
	}

	@Override
	public NewPredecessorMessage newMessageFor(ServentInfo next) {
		NewPredecessorMessage am = new NewPredecessorMessage(getSender(), next, newPredecessor);
		am.copyContextFrom(this);
		return am;
	}
}
