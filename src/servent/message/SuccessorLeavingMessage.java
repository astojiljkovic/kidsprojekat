package servent.message;

import app.ServentInfo;

public class SuccessorLeavingMessage extends SendAndForgetMessage {

	private final ServentInfo leaveInitiator;

	public SuccessorLeavingMessage(ServentInfo sender, ServentInfo receiver, ServentInfo leaveInitiator) {
		super(MessageType.SUCC_LEAVING, sender, receiver);
		this.leaveInitiator = leaveInitiator;
	}

	public ServentInfo getLeaveInitiator() {
		return leaveInitiator;
	}
}
