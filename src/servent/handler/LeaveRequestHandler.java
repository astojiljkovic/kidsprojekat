package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.*;
import servent.message.util.MessageUtil;

public class LeaveRequestHandler implements MessageHandler {

	private Message clientMessage;

	public LeaveRequestHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LEAVE_REQUEST) {
			LeaveRequestMessage leaveRequestMessage = (LeaveRequestMessage)clientMessage;
			
			AppConfig.chordState.handleLeave(leaveRequestMessage);
			
//			UpdateMessage um = new UpdateMessage(AppConfig.myServentInfo, AppConfig.chordState.getSuccessorInfo(), "");
//			MessageUtil.sendAndForgetMessage(um);
			
		} else {
			Logger.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
