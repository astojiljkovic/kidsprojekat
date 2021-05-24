package servent.message;

import java.util.Map;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private Map<Integer, Integer> values;
	
	public WelcomeMessage(int senderPort, String senderIp, int receiverPort, String senderTeam, String receiverIp, Map<Integer, Integer> values) {
		super(MessageType.WELCOME, senderIp, senderPort, senderTeam, receiverIp, receiverPort);
		
		this.values = values;
	}
	
	public Map<Integer, Integer> getValues() {
		return values;
	}
}
