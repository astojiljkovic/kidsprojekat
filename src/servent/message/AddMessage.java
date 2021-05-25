package servent.message;

import app.ServentInfo;

public class AddMessage extends BasicMessage {

	private static final long serialVersionUID = 5163039209888734276L;

	public AddMessage(ServentInfo sender, ServentInfo receiver, String name, String content) {
		super(MessageType.ADD, sender, receiver, name + "<=>" + content);
	}
}
