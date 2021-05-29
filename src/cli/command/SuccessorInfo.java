package cli.command;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;

public class SuccessorInfo implements CLICommand {

	@Override
	public String commandName() {
		return "successor_info";
	}

	@Override
	public void execute(String args) {
		ServentInfo[] successorTable = AppConfig.chordState.state.getFingerTable();

		System.out.println("Successors list");
		for(int i = 0; i < ChordState.State.MAX_SUCCESSORS; i++) {
			System.out.println("" + AppConfig.chordState.state.getSuccessorInfoForPrint()[i]);
		}

		int num = 0;
		System.out.println("Finger table");
		for (ServentInfo serventInfo : successorTable) {
			System.out.println(num + ": " + serventInfo);
			num++;
		}
	}

}
