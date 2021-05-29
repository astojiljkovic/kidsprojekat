package servent.message.util;

import app.Logger;
import servent.handler.ResponseMessageHandler;
import servent.message.Message;
import servent.message.SendAndForgetMessage;
import servent.message.TrackedMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.*;

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

	//TODO: removeFileForUs this duplicate
	public static void sendAndForgetMessage(TrackedMessage message) { //TODO: delete
		sendMessage(message);
	}

	private static final List<SentMessage> sentMessages = new ArrayList<>();
//	private static final Object sentMessagesListLock = new Object();

	public static Optional<ResponseMessageHandler> removeHandlerForId(int messageId) {
		Optional<ResponseMessageHandler> messageHandler = Optional.empty();

		synchronized(sentMessages) {

			for (SentMessage sentMessage : sentMessages) {
				if (sentMessage.getMessageId() == messageId) {
					messageHandler = sentMessage.getMessageHandlerForExecution();
					sentMessages.removeIf(sentMessage1 -> {
						return sentMessage1.getMessageId() == messageId;
					});
					break;
				}
			}

		}

		return messageHandler;
	}

	public static void sendTrackedMessageAwaitingResponse(TrackedMessage message, ResponseMessageHandler handler, long timeout, MessageTimeoutHandler timeoutHandler) {
		int messageId = message.getMessageId();

		SentMessage sentMessage = new SentMessage(messageId, handler, timeout, timeoutHandler);
		synchronized(sentMessages) {
			sentMessages.add(sentMessage);
		}

		sendMessage(message);
	}

	public static void sendTrackedMessageAwaitingResponse(TrackedMessage message, ResponseMessageHandler handler) {
		int messageId = message.getMessageId();

		SentMessage sentMessage = new SentMessage(messageId, handler);
		synchronized(sentMessages) {
			sentMessages.add(sentMessage);
		}

		sendMessage(message);
	}

	public interface MessageTimeoutHandler {
		/**
		 * Return number of milliseconds to extend timeout, or -1 if done
		 */
		long execute(int invocation);
	}
}
