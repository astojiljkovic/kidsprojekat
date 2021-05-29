package servent.handler.chord.leave;

import app.AppConfig;
import app.Logger;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

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
