package servent.message;

import app.ServentInfo;

public class UpdateMessage extends BasicMessage {

	private static final long serialVersionUID = 3586102505319194978L;

	public UpdateMessage(ServentInfo sender, ServentInfo receiver, String text) {
		super(MessageType.UPDATE, sender, receiver, text);
	}
}
