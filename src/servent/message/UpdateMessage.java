package servent.message;

import app.ServentInfo;

import java.util.List;

public class UpdateMessage extends ResponseMessage {

	private static final long serialVersionUID = 3586102505319194978L;
	private final List<ServentInfo> nodes;
	private final List<ServentInfo> removedNodes;

	public UpdateMessage(ServentInfo sender, ServentInfo receiver, List<ServentInfo> nodes, List<ServentInfo> removedNodes) {
		super(MessageType.UPDATE, sender, receiver, "");
		this.nodes = nodes;
		this.removedNodes = removedNodes;
	}

	public List<ServentInfo> getRemovedNodes() {
		return removedNodes;
	}

	public List<ServentInfo> getNodes() {
		return nodes;
	}

	@Override
	public UpdateMessage newMessageFor(ServentInfo next) {
		UpdateMessage am = new UpdateMessage(getSender(), next, nodes, removedNodes);
		am.copyContextFrom(this);
		return am;
	}
}
