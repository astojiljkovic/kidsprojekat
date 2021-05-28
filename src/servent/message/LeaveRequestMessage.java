package servent.message;

import app.ServentInfo;

public class LeaveRequestMessage extends SendAndForgetMessage {

	private final ServentInfo predecessor;

	public LeaveRequestMessage(ServentInfo sender, ServentInfo receiver, ServentInfo predecessor) {
		super(MessageType.LEAVE_REQUEST, sender, receiver);
		this.predecessor = predecessor;
	}

	public ServentInfo getPredecessor() {
		return predecessor;
	}
}
