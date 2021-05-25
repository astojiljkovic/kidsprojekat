package servent.message;

import app.ServentInfo;

import java.awt.image.AffineTransformOp;
import java.io.Serializable;

/**
 * This is your basic message. It should cover most needs.
 * It supports the following stuff:
 * <ul>
 * 	<li>Basic attributes:<ul>
 * 		<li>Message ID - unique on a single servent.</li>
 * 		<li>Message type</li>
 * 		<li>Sender port</li>
 * 		<li>Receiver port</li>
 * 		<li>Receiver IP address</li>
 * 		<li>Arbitrary message text</li>
 * 		</ul>
 * 	<li>Is serializable</li>
 * 	<li>Is immutable</li>
 * 	<li>Equality and hashability based on message id and original sender port</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public interface Message extends Serializable {

	ServentInfo getSender(); 
	ServentInfo getReceiver();

	/**
	 * Message type. Mainly used to decide which handler will work on this message.
	 */
	MessageType getMessageType();
	
	/**
	 * The body of the message. Use this to see what your neighbors have sent you.
	 */
	String getMessageText();
	
	/**
	 * An id that is unique per servent. Combined with servent id, it will be unique
	 * in the system.
	 */
	int getMessageId();

}
