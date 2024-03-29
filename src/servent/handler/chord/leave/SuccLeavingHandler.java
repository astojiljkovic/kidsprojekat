package servent.handler.chord.leave;

import app.AppConfig;
import app.Logger;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.chord.leave.SuccessorLeavingMessage;

public class SuccLeavingHandler implements MessageHandler {

	private Message clientMessage;

	public SuccLeavingHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SUCC_LEAVING) {
			SuccessorLeavingMessage message = (SuccessorLeavingMessage) clientMessage;

			AppConfig.chordState.handleSuccessorLeaving(message.getSender(), message.getLeaveInitiator());
		} else {
			Logger.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
