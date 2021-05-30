package servent.message.data;

import app.ServentInfo;
import app.storage.SillyGitStorageFile;
import servent.message.MessageType;
import servent.message.SendAndForgetMessage;
import servent.message.TrackedMessage;

import java.util.List;

public class RedundantCopyMessage extends TrackedMessage {
	private final ServentInfo replicationTarget;
	private final ServentInfo mainNode;
	private final List<SillyGitStorageFile> data;

	public RedundantCopyMessage(ServentInfo sender, ServentInfo receiver, ServentInfo mainNode, ServentInfo replicationTarget, List<SillyGitStorageFile> data) {
		super(MessageType.REDUNDANT_COPY, sender, receiver, "");
		this.mainNode = mainNode;
		this.replicationTarget = replicationTarget;
		this.data = data;
	}

	public ServentInfo getMainNode() {
		return mainNode;
	}

	public ServentInfo getReplicationTarget() {
		return replicationTarget;
	}

	public List<SillyGitStorageFile> getData() {
		return data;
	}

	@Override
	public RedundantCopyMessage newMessageFor(ServentInfo next) {
		RedundantCopyMessage am = new RedundantCopyMessage(getSender(), next, getMainNode(), getReplicationTarget(), getData());
		am.copyContextFrom(this);
		return am;
	}
}
