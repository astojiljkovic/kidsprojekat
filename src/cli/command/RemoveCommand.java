package cli.command;

import app.AppConfig;

public class RemoveCommand implements CLICommand {

    @Override
    public String commandName() {
        return "removeFileForUs";
    }

    @Override
    public void execute(String args) {
        String removePath = args;

        AppConfig.chordState.removeFilesForUs(removePath);
    }
}
