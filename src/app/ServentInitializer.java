package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import servent.message.NewNodeMessage;
import servent.message.util.MessageUtil;

public class ServentInitializer implements Runnable {

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

			NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo, someServentIp, someServentPort);
			MessageUtil.sendMessage(nnm);
		}
	}

}
