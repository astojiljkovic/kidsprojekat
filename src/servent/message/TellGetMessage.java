package servent.message;

import app.ServentInfo;

public class TellGetMessage extends BasicMessage {
	public static final String FILE_DOESNT_EXIST_CONTENT = "FAJL_NE_POSTOJI" ;

	private static final long serialVersionUID = -6213394344524749872L;

	public TellGetMessage(ServentInfo sender, ServentInfo receiver, String fileName, String content) {
		super(MessageType.TELL_GET, sender, receiver, fileName + "<=>" + content);
	}
}
