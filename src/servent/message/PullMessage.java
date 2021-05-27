package servent.message;

import app.ServentInfo;

public class PullMessage extends TrackedMessage {

	private static final long serialVersionUID = -8558031124520315033L;

	private final String fileName;
	private final int version;

	public PullMessage(ServentInfo sender, ServentInfo receiver, String fileName, int version) {
		super(MessageType.PULL, sender, receiver, "");
		this.fileName = fileName;
		this.version = version;
	}

	@Override
	public PullMessage newMessageFor(ServentInfo next) {
		PullMessage message = new PullMessage(getSender(), next, getFileName(), getVersion());
		message.copyContextFrom(this);
		return message;
	}

	public String getFileName() {
		return fileName;
	}

	public int getVersion() {
		return version;
	}
}
