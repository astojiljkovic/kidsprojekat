package servent.message;

import java.util.Map;
import app.ServentInfo;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private Map<Integer, Integer> values;
	
	public WelcomeMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> values) {
		super(MessageType.WELCOME, sender, receiver);
		
		this.values = values;
	}
	
	public Map<Integer, Integer> getValues() {
		return values;
	}
}
