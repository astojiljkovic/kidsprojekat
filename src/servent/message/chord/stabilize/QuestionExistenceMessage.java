package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.TrackedMessage;

public class QuestionExistenceMessage extends TrackedMessage {

	private ServentInfo softDeadNode;

	public QuestionExistenceMessage(ServentInfo sender, ServentInfo receiver, ServentInfo softDeadNode) {
		super(MessageType.QUESTION_EXISTENCE, sender, receiver, "");
		this.softDeadNode = softDeadNode;
	}

	public ServentInfo getSoftDeadNode() {
		return softDeadNode;
	}

	@Override
	public QuestionExistenceMessage newMessageFor(ServentInfo next) {
		QuestionExistenceMessage am = new QuestionExistenceMessage(getSender(), next, softDeadNode);
		am.copyContextFrom(this);
		return am;
	}
}
