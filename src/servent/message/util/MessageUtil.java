package servent.message.util;

import app.Logger;
import servent.handler.ResponseMessageHandler;
import servent.message.Message;
import servent.message.SendAndForgetMessage;
import servent.message.TrackedMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * 
 * Depending on the configuration it delegates sending either to a {@link DelayedMessageSender}
 * in a new thread (non-FIFO) or stores the message in a queue for the {@link FifoSendWorker} (FIFO).
 * 
 * When reading, if we are FIFO, we send an ACK message on the same socket, so the other side
 * knows they can send the next message.
 * @author bmilojkovic
 *
 */
public class MessageUtil {

	/**
	 * Normally this should be true, because it helps with debugging.
	 * Flip this to false to disable printing every message send / receive.
	 */
	public static final boolean MESSAGE_UTIL_PRINTING = true;
	
	public static Message readMessage(Socket socket) {
		
		Message clientMessage = null;
			
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
	
			clientMessage = (Message) ois.readObject();
			
			socket.close();
		} catch (IOException e) {
			Logger.timestampedErrorPrint("Error in reading socket on " +
					socket.getInetAddress() + ":" + socket.getPort());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (MESSAGE_UTIL_PRINTING) {
			Logger.timestampedStandardPrint("Got message " + clientMessage);
		}
				
		return clientMessage;
	}
	
	private static void sendMessage(Message message) {
		Thread delayedSender = new Thread(new DelayedMessageSender(message));
		
		delayedSender.start();
	}

	public static void sendAndForgetMessage(SendAndForgetMessage message) {
		sendMessage(message);
	}

	//TODO: remove this duplicate
	public static void sendAndForgetMessage(TrackedMessage message) { //TODO: delete
		sendMessage(message);
	}

	private static Map<Integer, ResponseMessageHandler> trackedHandlers = new ConcurrentHashMap<>();

	public static ResponseMessageHandler removeHandlerForId(int messageId) {
		return trackedHandlers.remove(messageId);
	}

	public static void sendTrackedMessageAwaitingResponse(TrackedMessage message, ResponseMessageHandler handler) {
		int messageId = message.getMessageId();
		trackedHandlers.put(messageId, handler);

		sendMessage(message);
	}
}
