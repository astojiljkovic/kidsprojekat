package servent.message;

import app.ServentInfo;
import app.SillyGitStorageFile;
import app.git.pull.RemoveResult;

import java.util.List;

public class RemoveResponseMessage extends ResponseMessage {

	private final List<SillyGitStorageFile> removeResult;
	private final String requestedPath;

	public RemoveResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, List<SillyGitStorageFile> removeResult) {
		super(MessageType.PULL_RESPONSE, sender, receiver, "");
		this.removeResult = removeResult;
		this.requestedPath = requestedPath;
	}

	@Override
	protected String additionalContentToPrint() {
		if (removeResult == null) {
			return "";
		}

		return removeResult.toString(); //TODO: fix print
	}

	public List<SillyGitStorageFile> getRemoveResult() {
		return removeResult;
	}

	public String getRequestedPath() {
		return requestedPath;
	}

	@Override
	public RemoveResponseMessage newMessageFor(ServentInfo next) {
		RemoveResponseMessage message = new RemoveResponseMessage(getSender(), next, requestedPath, removeResult);
		message.copyContextFrom(this);
		return message;
	}
}
