package servent.message;

import app.ServentInfo;

public class LeaveGrantedMessage extends SendAndForgetMessage {

	public LeaveGrantedMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.LEAVE_GRANTED, sender, receiver);
	}
}
