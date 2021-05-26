package servent.message;

import app.ServentInfo;
import app.SillyGitFile;
import app.SillyGitStorageFile;

public class TellGetMessage extends BasicMessage {

	private static final long serialVersionUID = -6213394344524749872L;
	private final SillyGitStorageFile sgsf;

	public TellGetMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, SillyGitStorageFile sgsf) {
		super(MessageType.TELL_GET, sender, receiver, requestedPath);
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
}
