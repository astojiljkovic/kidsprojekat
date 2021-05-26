package servent.message;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import app.ChordState;
import app.ServentInfo;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modificator methods.
 * If you don't override the modificators, you might drop stuff.
 * @author bmilojkovic
 *
 */
public class BasicMessage implements Message {

	private static final long serialVersionUID = -9075856313609777945L;
	private final MessageType type;

//	Sender info
	private final ServentInfo sender;

//	 Receiver info
	private final ServentInfo receiver;
	private final String messageText;
	
	//This gives us a unique id - incremented in every natural constructor.
	private static final AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId = messageCounter.getAndIncrement();

	public BasicMessage(MessageType type, ServentInfo sender, ServentInfo receiver) {
		this(type, sender, receiver, "");
	}

	public BasicMessage(MessageType type, ServentInfo sender, ServentInfo receiver, String messageText) {
		this.type = type;
		this.sender = sender;
		this.receiver = receiver;
		this.messageText = messageText;
	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}

	@Override
	public ServentInfo getSender() {
		return sender;
	}

	@Override
	public ServentInfo getReceiver() {
		return receiver;
	}

	@Override
	public String getMessageText() {
		return messageText;
	}
	
	@Override
	public int getMessageId() {
		return messageId;
	}
	
	/**
	 * Comparing messages is based on their unique id and the original sender location.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage) {
			BasicMessage other = (BasicMessage)obj;
			
			if (getMessageId() == other.getMessageId() &&
					getSender().getNetworkLocation().equals(other.getSender().getNetworkLocation())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are gonna keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), sender.getNetworkLocation());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|sender_port|message_id|text|type|receiver_port|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[" + getSender().getChordId() + "(" + getSender().getTeam() + ")" + "|" + getSender().getNetworkLocation() + "|" + getMessageId() + "|" +
				getMessageText() + "|" + additionalContentToPrint() + "|" + getMessageType() + "|" +
				getReceiver().getNetworkLocation() + "|" +
				getReceiver().getChordId() + "(" + getReceiver().getTeam() + ")" + "]";
	}

	protected String additionalContentToPrint() { return ""; }
}
