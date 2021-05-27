package servent.message;

import app.ServentInfo;
import app.SillyGitFile;

public class AddMessage extends TrackedMessage {

	private static final long serialVersionUID = 5163039209888734276L;

	private final SillyGitFile sgf;

	public AddMessage(ServentInfo sender, ServentInfo receiver, SillyGitFile file) {
		super(MessageType.ADD, sender, receiver, "");
		this.sgf = file;
	}

	public SillyGitFile getSgf() {
		return sgf;
	}

	@Override
	protected String additionalContentToPrint() {
		return sgf.getPathInWorkDir() + "|" + sgf.getContent() + "|" + sgf.getStorageHash().orElse("");
	}

	@Override
	public AddMessage newMessageFor(ServentInfo next) {
		AddMessage am = new AddMessage(getSender(), next, sgf);
		am.copyContextFrom(this);
		return am;
	}

}
