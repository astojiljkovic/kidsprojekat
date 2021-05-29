package servent.message.chord.leave;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.SendAndForgetMessage;

public class LeaveGrantedMessage extends SendAndForgetMessage {

	public LeaveGrantedMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.LEAVE_GRANTED, sender, receiver);
	}
}
