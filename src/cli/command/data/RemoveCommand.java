package cli.command.data;

import app.AppConfig;
import cli.command.CLICommand;

public class RemoveCommand implements CLICommand {

    @Override
    public String commandName() {
        return "remove";
    }

    @Override
    public void execute(String args) {
        String removePath = args;

        AppConfig.chordState.removeFilesForUs(removePath);
    }
}
