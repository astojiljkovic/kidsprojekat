package cli.command.data;

import app.AppConfig;
import app.Logger;
import app.merge.NotReadyForInputException;
import app.storage.FileAlreadyAddedStorageException;
import cli.command.CLICommand;

import java.io.FileNotFoundException;

public class ViewCommand implements CLICommand {

    @Override
    public String commandName() {
        return "view";
    }

    @Override
    public void execute(String args) {

        try {
            AppConfig.mergeResolver.view();
        } catch (NotReadyForInputException e) {
            Logger.timestampedErrorPrint("Invalid command 'view' - Merge resolver not ready for input");
        }

    }

}
