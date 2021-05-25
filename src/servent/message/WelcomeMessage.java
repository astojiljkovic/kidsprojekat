package servent.message;

import java.util.Map;
import app.ServentInfo;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private Map<String, String> values;
	
	public WelcomeMessage(ServentInfo sender, ServentInfo receiver, Map<String, String> values) {
		super(MessageType.WELCOME, sender, receiver);
		
		this.values = values;
	}
	
	public Map<String, String> getValues() {
		return values;
	}
}
