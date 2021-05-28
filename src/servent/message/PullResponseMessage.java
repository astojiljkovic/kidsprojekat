package servent.message;

import app.ServentInfo;
import app.git.pull.RemoveResult;

public class PullResponseMessage extends ResponseMessage {

	private static final long serialVersionUID = -6213394344524749872L;
	private final RemoveResult removeResult;

	public PullResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, RemoveResult removeResult) {
		super(MessageType.PULL_RESPONSE, sender, receiver, requestedPath);
		this.removeResult = removeResult;
	}

	@Override
	protected String additionalContentToPrint() {
		if (removeResult == null) {
			return "";
		}

		return removeResult.toString();
	}

	public RemoveResult getPullResult() {
		return removeResult;
	}

	@Override
	public PullResponseMessage newMessageFor(ServentInfo next) {
		PullResponseMessage message = new PullResponseMessage(getSender(), next, getMessageText(), removeResult);
		message.copyContextFrom(this);
		return message;
	}
}
