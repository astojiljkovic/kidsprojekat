package servent.message;

import java.util.List;
import java.util.Map;
import app.ServentInfo;
import app.SillyGitFile;
import app.SillyGitStorageFile;

public class WelcomeMessage extends SendAndForgetMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private List<SillyGitStorageFile> files;
	
	public WelcomeMessage(ServentInfo sender, ServentInfo receiver, List<SillyGitStorageFile> files) {
		super(MessageType.WELCOME, sender, receiver);

		this.files = files;
	}

	public List<SillyGitStorageFile> getFiles() {
		return files;
	}
}
