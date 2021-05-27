package cli.command;

import app.AppConfig;
import app.Logger;
import app.merge.NotReadyForInputException;

public class PushCommand implements CLICommand {

    @Override
    public String commandName() {
        return "push";
    }

    @Override
    public void execute(String args) {
        try {
            AppConfig.mergeResolver.push();
        } catch (NotReadyForInputException e) {
            Logger.timestampedErrorPrint("Invalid command 'push' - Merge resolver not ready for input");
        }

    }

}
