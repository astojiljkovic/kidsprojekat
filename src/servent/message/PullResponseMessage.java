package servent.message;

import app.ServentInfo;
import app.SillyGitStorageFile;

public class PullResponseMessage extends TrackedMessage {

	private static final long serialVersionUID = -6213394344524749872L;
	private final SillyGitStorageFile sgsf;

	public PullResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, SillyGitStorageFile sgsf) {
		super(MessageType.PULL_RESPONSE, sender, receiver, requestedPath);
		this.sgsf = sgsf;
	}

	@Override
	protected String additionalContentToPrint() {
		if (sgsf == null) {
			return "";
		}
		return sgsf.getPathInStorageDir() + "|" + sgsf.getContent() + "|" + sgsf.getVersionHash();
	}

	public SillyGitStorageFile getSgsf() {
		return sgsf;
	}

	@Override
	public PullResponseMessage newMessageFor(ServentInfo next) {
		PullResponseMessage message = new PullResponseMessage(getSender(), next, getMessageText(), getSgsf());
		message.copyContextFrom(this);
		return message;
	}
}
