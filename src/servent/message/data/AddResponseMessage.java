package servent.message.data;

import app.ServentInfo;
import app.git.add.AddResult;
import servent.message.MessageType;
import servent.message.ResponseMessage;

public class AddResponseMessage extends ResponseMessage {
    private final AddResult addResult;

    public AddResponseMessage(ServentInfo sender, ServentInfo receiver, AddResult addResult) {
        super(MessageType.ADD_RESPONSE, sender, receiver, "");
        this.addResult = addResult;
    }

    @Override
    protected String additionalContentToPrint() {
        return addResult.toString();
    }

    public AddResult getAddResult() {
        return addResult;
    }

    @Override
    public AddResponseMessage newMessageFor(ServentInfo next) {
        AddResponseMessage message = new AddResponseMessage(getSender(), next, addResult);
        message.copyContextFrom(this);
        return message;
    }
}
