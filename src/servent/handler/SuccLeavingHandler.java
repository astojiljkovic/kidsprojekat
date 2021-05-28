package servent.handler;

import app.AppConfig;
import app.Logger;
import com.sun.net.httpserver.Authenticator;
import servent.message.LeaveRequestMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SuccessorLeavingMessage;

public class SuccLeavingHandler implements MessageHandler {

	private Message clientMessage;

	public SuccLeavingHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SUCC_LEAVING) {
			SuccessorLeavingMessage message = (SuccessorLeavingMessage) clientMessage;

			AppConfig.chordState.handleSuccessorLeaving(message.getLeaveInitiator());
		} else {
			Logger.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
