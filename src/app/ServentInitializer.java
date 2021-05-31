package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

import app.storage.SillyGitStorageFile;
import servent.handler.ResponseMessageHandler;
import servent.handler.WelcomeHandler;
import servent.message.NewNodeMessage;
import servent.message.WelcomeMessage;
import servent.message.chord.BusyMessage;
import servent.message.chord.RequestLockMessage;
import servent.message.chord.leave.LeaveRequestMessage;
import servent.message.util.MessageUtil;

import static app.AppConfig.myServentInfo;
import static app.AppConfig.storage;

public class ServentInitializer implements Runnable {

	public static void notifyBootstrapAboutLeaving() {
		int bsPort = AppConfig.BOOTSTRAP_PORT;
		String bsIp = AppConfig.BOOTSTRAP_IP;

		try {
			Socket bsSocket = new Socket(bsIp, bsPort);

			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			//Send to bootstrap:
			//Hail\n
			//address\n
			//port\n
			bsWriter.write("Bye\n" + AppConfig.myServentInfo.getNetworkLocation().getIp() + "\n" + AppConfig.myServentInfo.getNetworkLocation().getPort() + "\n");
			bsWriter.flush();

			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getSomeServentLocation() {
		int bsPort = AppConfig.BOOTSTRAP_PORT;
		String bsIp = AppConfig.BOOTSTRAP_IP;
		
		String retVal = "-2";
		
		try {
			Socket bsSocket = new Socket(bsIp, bsPort);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			//Send to bootstrap:
			//Hail\n
			//address\n
			//port\n
			bsWriter.write("Hail\n" + AppConfig.myServentInfo.getNetworkLocation().getIp() + "\n" + AppConfig.myServentInfo.getNetworkLocation().getPort() + "\n");
			bsWriter.flush();
			
			Scanner bsScanner = new Scanner(bsSocket.getInputStream());
			String receivedFirstOrPort = String.valueOf(bsScanner.nextInt()); // -1 or PORT
			bsScanner.nextLine();

			if (receivedFirstOrPort.equals("-1")) { // we are first
				retVal = receivedFirstOrPort;
			} else { // received port
				String receivedIp = bsScanner.nextLine();
				retVal = receivedIp + ":" + receivedFirstOrPort;
			}
			
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	@Override
	public void run() {
		String someServentLocation = getSomeServentLocation();
		
		if (someServentLocation.equals("-2")) {
			Logger.timestampedErrorPrint("Error in contacting bootstrap. Exiting...");
			System.exit(0);
		}
		if (someServentLocation.equals("-1")) { //bootstrap gave us -1 -> we are first
			Logger.timestampedStandardPrint("First node in Chord system.");
		} else { //bootstrap gave us something else - let that node tell our successor that we are here
			System.out.println("BRZI TEST: " + someServentLocation);

			String someServentIp = someServentLocation.split(":")[0];
			int someServentPort = Integer.parseInt(someServentLocation.split(":")[1]);

			initiateChordEntry(someServentIp, someServentPort);
		}
	}

	private void initiateChordEntry(String someServentIp, int someServentPort) {
		RequestLockMessage rlm = new RequestLockMessage(AppConfig.myServentInfo, new ServentInfo(someServentIp, someServentPort, "UNKNOWN"), AppConfig.myServentInfo);
		MessageUtil.sendTrackedMessageAwaitingResponse(rlm, new ResponseMessageHandler() {
			@Override
			public void run() {
				if(message instanceof BusyMessage) { //Busy handler
					Logger.timestampedStandardPrint("Node currently busy, will retry in 2s " + someServentIp + ":" + someServentPort);
					try {
						Thread.sleep(2000);
						initiateChordEntry(message.getSender().getNetworkLocation().getIp(), message.getSender().getNetworkLocation().getPort());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else { //OK, Lock granted
					NewNodeMessage newNodeMessage = new NewNodeMessage(AppConfig.myServentInfo, message.getSender());
					MessageUtil.sendAndForgetMessage(newNodeMessage);//, new WelcomeHandler(message));
//					synchronized (state) {
//						synchronized (storage) {
//							List<String> allFileNames = storage.getAllStoredUnversionedFileNamesRelativeToStorageRoot();
//							List<SillyGitStorageFile> data = storage.removeFilesOnRelativePathsReturningGitFiles(allFileNames);
//							state.forwardToNode = nodeToHandleLeave;
//							LeaveRequestMessage lrm = new LeaveRequestMessage(myServentInfo, nodeToHandleLeave, state.getPredecessor(), data);
//							MessageUtil.sendAndForgetMessage(lrm);
//						}
//					}
				}
			}
		});
//		NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo, someServentIp, someServentPort);
//		MessageUtil.sendTrackedMessageAwaitingResponse(nnm, new ResponseMessageHandler() { //Handles Busy message, WelcomeHandler still goes to old HandlerListener (SimpleServentListener)
//			@Override
//			public void run() {
//				try {
//					if ( message instanceof BusyMessage) {
//						Logger.timestampedStandardPrint("Node currently busy, will retry in 2s " + someServentIp + ":" + someServentPort);
//						Thread.sleep(2000);
////						initiateNewNodeMsg(someServentIp, someServentPort);
//						initiateNewNodeMsg(message.getSender().getNetworkLocation().getIp(), message.getSender().getNetworkLocation().getPort());
//					} else { //Welcome message <= never enters here (see comment ^)
//						new WelcomeHandler(message);
//					}
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		});
	}

}
