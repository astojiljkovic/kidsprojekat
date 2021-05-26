package servent.message;

import java.util.List;
import java.util.Map;
import app.ServentInfo;
import app.SillyGitFile;
import app.SillyGitStorageFile;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

//	private Map<String, String> values;
	private List<SillyGitStorageFile> files;
	
	public WelcomeMessage(ServentInfo sender, ServentInfo receiver, List<SillyGitStorageFile> files) {//Map<String, String> values) {
		super(MessageType.WELCOME, sender, receiver);

		this.files = files;
//		this.values = values;
	}

	public List<SillyGitStorageFile> getFiles() {
		return files;
	}
	
//	public Map<String, String> getValues() {
//		return values;
//	}
}
