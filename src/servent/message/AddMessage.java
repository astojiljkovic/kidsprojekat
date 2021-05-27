package servent.message;

import app.ServentInfo;
import app.SillyGitFile;

import java.util.List;
import java.util.stream.Collectors;

public class AddMessage extends TrackedMessage {

	private static final long serialVersionUID = 5163039209888734276L;

	private final List<SillyGitFile> sillyGitFiles;

	public AddMessage(ServentInfo sender, ServentInfo receiver, List<SillyGitFile> sillyGitFiles) {
		super(MessageType.ADD, sender, receiver, "");
		this.sillyGitFiles = sillyGitFiles;
	}

	public List<SillyGitFile> getSillyGitFiles() {
		return sillyGitFiles;
	}

	@Override
	protected String additionalContentToPrint() {
		return sillyGitFiles.stream()
				.map(sillyGitFile -> {
					return "<" + sillyGitFile.getPathInWorkDir() + "|" + sillyGitFile.getContent() + "|" + sillyGitFile.getStorageHash().orElse("") + ">";
				})
				.collect(Collectors.joining());
	}

	@Override
	public AddMessage newMessageFor(ServentInfo next) {
		AddMessage am = new AddMessage(getSender(), next, sillyGitFiles);
		am.copyContextFrom(this);
		return am;
	}

}
