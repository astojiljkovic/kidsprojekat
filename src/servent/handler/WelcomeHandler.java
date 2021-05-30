package servent.handler;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.List;

public class WelcomeHandler implements MessageHandler {

	private Message clientMessage;
	
	public WelcomeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.WELCOME) {
			WelcomeMessage welcomeMsg = (WelcomeMessage)clientMessage;
			
			AppConfig.chordState.init(welcomeMsg);

			System.out.println("My Servent info in Welcome handler " + AppConfig.myServentInfo);

//			try { //Sleep to have more time to activate two cvors at the same time
//				Thread.sleep(10000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			UpdateMessage um = new UpdateMessage(AppConfig.myServentInfo, AppConfig.chordState.state.getClosestSuccessor(), List.of(AppConfig.myServentInfo), Collections.emptyList());
			MessageUtil.sendTrackedMessageAwaitingResponse(um, new UpdateHandler(welcomeMsg.getSender()));
			
		} else {
			Logger.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
