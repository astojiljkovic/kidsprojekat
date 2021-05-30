package cli.command;

import app.AppConfig;
import app.ChordState;
import app.Logger;
import cli.CLIParser;
import servent.SimpleServentListener;

public class StopCommand implements CLICommand {

	private CLIParser parser;
	private SimpleServentListener listener;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener) {
		this.parser = parser;
		this.listener = listener;
	}
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		Logger.timestampedStandardPrint("Stopping...");
		AppConfig.chordState.requestLeave(integer -> {

			listener.stop();
		});

		parser.stop();
	}
}
