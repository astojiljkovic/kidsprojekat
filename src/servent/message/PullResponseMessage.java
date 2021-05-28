package servent.message;

import app.ServentInfo;
import app.SillyGitStorageFile;
import app.git.pull.PullResult;

import java.util.List;
import java.util.stream.Collectors;

public class PullResponseMessage extends ResponseMessage {

	private static final long serialVersionUID = -6213394344524749872L;
	private final PullResult pullResult;

	public PullResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, PullResult pullResult) {
		super(MessageType.PULL_RESPONSE, sender, receiver, requestedPath);
		this.pullResult = pullResult;
	}

	@Override
	protected String additionalContentToPrint() {
		if (pullResult == null) {
			return "";
		}

		return pullResult.toString();
	}

	public PullResult getPullResult() {
		return pullResult;
	}

	@Override
	public PullResponseMessage newMessageFor(ServentInfo next) {
		PullResponseMessage message = new PullResponseMessage(getSender(), next, getMessageText(), pullResult);
		message.copyContextFrom(this);
		return message;
	}
}
