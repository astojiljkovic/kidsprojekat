package servent.message;

import app.ServentInfo;

import java.util.List;

public class UpdateMessage extends SendAndForgetMessage {

	private static final long serialVersionUID = 3586102505319194978L;
	private final List<ServentInfo> nodes;

	public UpdateMessage(ServentInfo sender, ServentInfo receiver, List<ServentInfo> nodes) {
		super(MessageType.UPDATE, sender, receiver, "");
		this.nodes = nodes;
	}

	public List<ServentInfo> getNodes() {
		return nodes;
	}
}
