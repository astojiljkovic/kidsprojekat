package cli.command;

import app.AppConfig;
import app.Logger;
import app.merge.NotReadyForInputException;

public class AbortCommand implements CLICommand {
    @Override
    public String commandName() {
        return "abort";
    }

    @Override
    public void execute(String args) {
        if (args != null || !args.isEmpty()) {

            try {
                AppConfig.mergeResolver.abort();
            } catch (NotReadyForInputException e) {
                Logger.timestampedErrorPrint("Invalid command 'view' - Merge resolver not ready for input");
            }
        } else {
            Logger.timestampedErrorPrint("Invalid command for abort - " + args);
        }
    }
}
