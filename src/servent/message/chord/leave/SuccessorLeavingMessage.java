package servent.message.chord.leave;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.SendAndForgetMessage;

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
