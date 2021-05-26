package servent.message;

import app.ServentInfo;

public class AskGetMessage extends BasicMessage {

	private static final long serialVersionUID = -8558031124520315033L;

	public AskGetMessage(ServentInfo sender, ServentInfo receiver, String fileName, int version) {
		super(MessageType.ASK_GET, sender, receiver, fileName + " " + version);
	}
}
