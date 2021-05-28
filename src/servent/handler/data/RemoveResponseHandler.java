package servent.handler.data;

import app.AppConfig;
import app.Logger;
import app.SillyGitStorageFile;
import servent.handler.ResponseMessageHandler;
import servent.message.MessageType;
import servent.message.data.RemoveResponseMessage;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveResponseHandler extends ResponseMessageHandler {

	@Override
	public void run() {
		if (message.getMessageType() == MessageType.REMOVE_RESPONSE) {
			RemoveResponseMessage responseMessage = (RemoveResponseMessage) message;

			List<SillyGitStorageFile> addResult = responseMessage.getRemoveResult();

			for(SillyGitStorageFile sgsf: addResult) {
				AppConfig.workDirectory.removeFileForPath(sgsf.getPathInStorageDir());
			}

			Logger.timestampedStandardPrint("Remote Remove completed!");
			Logger.timestampedStandardPrint("Results:");
			Logger.timestampedStandardPrint("Success - " + addResult.stream().map(SillyGitStorageFile::getPathInStorageDir).collect(Collectors.joining(" ")));

		} else {
			Logger.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}