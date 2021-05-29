package servent.message;

import java.util.List;
import java.util.Map;
import app.ServentInfo;
import app.SillyGitFile;
import app.SillyGitStorageFile;

public class WelcomeMessage extends SendAndForgetMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private List<ServentInfo> successors;
	private List<SillyGitStorageFile> files;
	
	public WelcomeMessage(ServentInfo sender, ServentInfo receiver, List<SillyGitStorageFile> files, List<ServentInfo> successors) {
		super(MessageType.WELCOME, sender, receiver);

		this.files = files;
		this.successors = successors;
	}

	public List<ServentInfo> getSuccessors() {
		return successors;
	}

	public List<SillyGitStorageFile> getFiles() {
		return files;
	}
}
