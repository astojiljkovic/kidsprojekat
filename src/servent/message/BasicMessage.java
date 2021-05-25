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
//	private final NetworkLocation senderLocation;
//	private final String senderTeam;

//	 Receiver info
	private final ServentInfo receiver;
//	private final NetworkLocation receiverLocation;
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
	
//	public BasicMessage(MessageType type, String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort) {
//		this.type = type;
//		this.sender = new ServentInfo(senderIp, senderPort, senderTeam);
//		this.receiver = new ServentInfo(senderIp, senderPort, senderTeam);
////		this.senderLocation = new NetworkLocation(senderIp, senderPort);
////		this.senderTeam = senderTeam;
//		this.receiverLocation = new NetworkLocation(receiverIp, receiverPort);
//		this.messageText = "";
//
//		this.messageId = messageCounter.getAndIncrement();
//	}
	
//	public BasicMessage(MessageType type, String senderIp, int senderPort, String senderTeam, String receiverIp, int receiverPort, String messageText) {
//		this.type = type;
//		this.senderLocation = new NetworkLocation(senderIp, senderPort);
//		this.senderTeam = senderTeam;
//		this.receiverLocation = new NetworkLocation(receiverIp, receiverPort);
//		this.messageText = messageText;
//
//		this.messageId = messageCounter.getAndIncrement();
//	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}
	
//	@Override
//	public int getReceiverPort() {
//		return receiverPort;
//	}
//
//	@Override
//	public String getReceiverIpAddress() {
//		return receiverIp;
//	}
	@Override
	public NetworkLocation getReceiverLocation() {
		return receiver.getNetworkLocation();
	}

	@Override
	public String getSenderTeam() {
		return sender.getTeam();
	}

	@Override
	public NetworkLocation getSenderLocation() {
		return sender.getNetworkLocation();
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
					getSenderLocation().equals(other.getSenderLocation())) {
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
		return Objects.hash(getMessageId(), getSenderLocation());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|sender_port|message_id|text|type|receiver_port|receiver_id]</code>
	 */
//	@Override
//	public String toString() {
//		return "[" + ChordState.chordHash(getSenderPort()) + "|" + getSenderPort() + "|" + getMessageId() + "|" +
//					getMessageText() + "|" + getMessageType() + "|" +
//					getReceiverPort() + "|" + ChordState.chordHash(getReceiverPort()) + "]";
//	}

	@Override
	public String toString() {
		return "[" + getSenderLocation() + "|" + getMessageId() + "|" +
				getMessageText() + "|" + getMessageType() + "|" +
				getReceiverLocation() + "]";

	}
}
