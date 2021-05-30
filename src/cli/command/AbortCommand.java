package cli.command;

import app.AppConfig;
import app.Logger;
import app.merge.NotReadyForInputException;

public class AbortCommand implements CLICommand { //TODO: remove
    @Override
    public String commandName() {
        return "lock";
    }

    @Override
    public void execute(String args) {
        if(args.equals("yes")) {
//            AppConfig.mergeResolver.abort();
            Logger.timestampedStandardPrint("Trying to get my lock");
            while(!AppConfig.chordState.state.acquireBalancingLock(AppConfig.myServentInfo.getChordId())) {
                Logger.timestampedStandardPrint("Couldn't get my lock, going to sleep");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Logger.timestampedStandardPrint("Lock acquired");
        }
        if (args.equals("no")) {
            AppConfig.chordState.state.releaseBalancingLock(AppConfig.myServentInfo.getChordId());
        }
    }
}
