package cli.command;

import app.AppConfig;
import app.Logger;

public class InfoCommand implements CLICommand {

	@Override
	public String commandName() {
		return "info";
	}

	@Override
	public void execute(String args) {
		Logger.timestampedStandardPrint("My info: " + AppConfig.myServentInfo);
	}

}
