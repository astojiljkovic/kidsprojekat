package servent.message.chord;

import app.ServentInfo;
import servent.message.MessageType;
import servent.message.ResponseMessage;

public class BusyMessage extends ResponseMessage {

//	private boolean isAcquired;

	public BusyMessage(ServentInfo sender, ServentInfo receiver) {
		super(MessageType.BUSY, sender, receiver, "");
//		this.isAcquired = isAcquired;
	}

//	public boolean isAcquired() {
//		return isAcquired;
//	}

	@Override
	public BusyMessage newMessageFor(ServentInfo next) {
		BusyMessage am = new BusyMessage(getSender(), next);
		am.copyContextFrom(this);
		return am;
	}
}
