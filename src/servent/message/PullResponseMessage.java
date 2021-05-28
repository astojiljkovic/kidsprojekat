package servent.message;

import app.ServentInfo;
import app.SillyGitStorageFile;

import java.util.List;
import java.util.stream.Collectors;

public class PullResponseMessage extends ResponseMessage {

	private static final long serialVersionUID = -6213394344524749872L;
	private final List<SillyGitStorageFile> sillyGitStorageFiles;

	public PullResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, List<SillyGitStorageFile> sillyGitStorageFiles) {
		super(MessageType.PULL_RESPONSE, sender, receiver, requestedPath);
		this.sillyGitStorageFiles = sillyGitStorageFiles;
	}

	@Override
	protected String additionalContentToPrint() {
		if (sillyGitStorageFiles == null) {
			return "";
		}

		return sillyGitStorageFiles.stream().map(sillyGitStorageFile -> {
			return "<" + sillyGitStorageFile.getPathInStorageDir() + "|" + sillyGitStorageFile.getContent() + "|" + sillyGitStorageFile.getVersionHash() + ">";
		}).collect(Collectors.joining(""));
	}

	public List<SillyGitStorageFile> getSillyGitStorageFiles() {
		return sillyGitStorageFiles;
	}

	@Override
	public PullResponseMessage newMessageFor(ServentInfo next) {
		PullResponseMessage message = new PullResponseMessage(getSender(), next, getMessageText(), getSillyGitStorageFiles());
		message.copyContextFrom(this);
		return message;
	}
}
