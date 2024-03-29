package servent.message.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import app.AppConfig;
import app.Logger;
import servent.message.Message;
import servent.message.MessageType;

import static servent.message.util.MessageUtil.MESSAGE_UTIL_PING_PRINTING;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or somesuch.
 * 
 * @author bmilojkovic
 *
 */
public class DelayedMessageSender implements Runnable {

	private Message messageToSend;
	
	public DelayedMessageSender(Message messageToSend) {
		this.messageToSend = messageToSend;
	}
	
	public void run() {
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 100) + 100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		if (MessageUtil.MESSAGE_UTIL_PRINTING) {
			if (MESSAGE_UTIL_PING_PRINTING || (!(messageToSend.getMessageType() == MessageType.PONG || messageToSend.getMessageType() == MessageType.PING))) {
				Logger.timestampedStandardPrint("Sending message " + messageToSend);
			}
		}
		
		try {
			Socket sendSocket = new Socket(messageToSend.getReceiver().getNetworkLocation().getIp(), messageToSend.getReceiver().getNetworkLocation().getPort());
			
			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			oos.writeObject(messageToSend);
			oos.flush();
			
			sendSocket.close();
		} catch (IOException e) {
			Logger.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString());
		}
	}
	
}
