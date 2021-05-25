package servent.message;

import app.ServentInfo;

public class TellGetMessage extends BasicMessage {

	private static final long serialVersionUID = -6213394344524749872L;

	public TellGetMessage(ServentInfo sender, ServentInfo receiver, String fileName, String content) {
		super(MessageType.TELL_GET, sender, receiver, fileName + "<=>" + content);
	}
}
