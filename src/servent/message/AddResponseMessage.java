package servent.message;

import app.ServentInfo;
import app.SillyGitStorageFile;

public class AddResponseMessage extends ResponseMessage {
    private final SillyGitStorageFile sgsf;

    public AddResponseMessage(ServentInfo sender, ServentInfo receiver, String requestedPath, SillyGitStorageFile sgsf) {
        super(MessageType.ADD_RESPONSE, sender, receiver, requestedPath);
        this.sgsf = sgsf;
    }

    @Override
    protected String additionalContentToPrint() {
        if (sgsf == null) {
            return "";
        }
        return sgsf.getPathInStorageDir() + "|" + sgsf.getContent() + "|" + sgsf.getVersionHash();
    }

    public SillyGitStorageFile getSgsf() {
        return sgsf;
    }

    @Override
    public AddResponseMessage newMessageFor(ServentInfo next) {
        AddResponseMessage message = new AddResponseMessage(getSender(), next, getMessageText(), sgsf);
        message.copyContextFrom(this);
        return message;
    }
}
