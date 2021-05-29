package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.ResponseMessage;

import java.util.List;

public class NewPredecessorResponseMessage extends ResponseMessage {

	private List<ServentInfo> successors;

	public NewPredecessorResponseMessage(ServentInfo sender, ServentInfo receiver, List<ServentInfo> successors) {
		super(MessageType.NEW_PREDECESSOR_RESPONSE, sender, receiver, "");
		this.successors = successors;
	}

	public List<ServentInfo> getSuccessors() {
		return successors;
	}

	@Override
	public NewPredecessorResponseMessage newMessageFor(ServentInfo next) {
		NewPredecessorResponseMessage am = new NewPredecessorResponseMessage(getSender(), next, successors);
		am.copyContextFrom(this);
		return am;
	}
}
