package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SuccessorLeavingMessage;

public class LeaveGrantedHandler implements MessageHandler {

	private Message clientMessage;

	public LeaveGrantedHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LEAVE_GRANTED) {

			AppConfig.chordState.handleLeaveGranted();
		} else {
			Logger.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
