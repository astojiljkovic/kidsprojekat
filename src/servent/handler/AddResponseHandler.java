package servent.handler;

import app.AppConfig;
import app.Logger;
import app.SillyGitStorageFile;
import app.git.add.AddResult;
import servent.message.AddResponseMessage;
import servent.message.MessageType;

import java.util.stream.Collectors;

public class AddResponseHandler extends ResponseMessageHandler {

	@Override
	public void run() {
		if (message.getMessageType() == MessageType.ADD_RESPONSE) {
			AddResponseMessage responseMessage = (AddResponseMessage) message;

			AddResult addResult = responseMessage.getAddResult();

			for(SillyGitStorageFile sgsf: addResult.getSuccesses()) {
				AppConfig.workDirectory.addFile(sgsf.getPathInStorageDir(), sgsf.getContent(), sgsf.getVersionHash());
			}

			Logger.timestampedStandardPrint("Remote add completed!");
			Logger.timestampedStandardPrint("Results:");
			Logger.timestampedStandardPrint("Success - " + addResult.getSuccesses().stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" ")));
			Logger.timestampedStandardPrint("Failures - " + String.join(" ", addResult.getFailedPaths()));

		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}