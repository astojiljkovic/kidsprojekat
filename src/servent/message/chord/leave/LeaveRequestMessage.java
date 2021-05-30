package servent.message.chord.leave;

import app.ServentInfo;
import app.storage.SillyGitStorageFile;
import servent.message.MessageType;
import servent.message.SendAndForgetMessage;

import java.util.List;

public class LeaveRequestMessage extends SendAndForgetMessage {

	private final ServentInfo predecessor;
	private final List<SillyGitStorageFile> data;

	public LeaveRequestMessage(ServentInfo sender, ServentInfo receiver, ServentInfo predecessor, List<SillyGitStorageFile> data) {
		super(MessageType.LEAVE_REQUEST, sender, receiver);
		this.predecessor = predecessor;
		this.data = data;
	}

	public ServentInfo getPredecessor() {
		return predecessor;
	}

	public List<SillyGitStorageFile> getData() {
		return data;
	}
}
