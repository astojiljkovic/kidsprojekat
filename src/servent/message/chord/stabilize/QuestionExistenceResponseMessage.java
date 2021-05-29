package servent.message.chord.stabilize;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.ResponseMessage;

public class QuestionExistenceResponseMessage extends ResponseMessage {

	private boolean isDead;
	private ServentInfo node;

	public QuestionExistenceResponseMessage(ServentInfo sender, ServentInfo receiver, boolean isDead, ServentInfo node) {
		super(MessageType.QUESTION_EXISTENCE_RESPONSE, sender, receiver, "");
		this.node = node;
		this.isDead = isDead;
	}

	public boolean isDead() {
		return isDead;
	}

	public ServentInfo getNode() {
		return node;
	}

	@Override
	public QuestionExistenceResponseMessage newMessageFor(ServentInfo next) {
		QuestionExistenceResponseMessage am = new QuestionExistenceResponseMessage(getSender(), next, isDead, node);
		am.copyContextFrom(this);
		return am;
	}
}
