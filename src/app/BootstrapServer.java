package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BootstrapServer {

	class ServentLocation {
		private final String ip;
		private final int port;

		public ServentLocation(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		public String getIp() {
			return ip;
		}

		public int getPort() {
			return port;
		}
	}

	private volatile boolean working = true;
	private List<ServentLocation> activeServents;
	
	private class CLIWorker implements Runnable {
		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			
			String line;
			while(true) {
				line = sc.nextLine();
				
				if (line.equals("stop")) {
					working = false;
					break;
				}
			}
			
			sc.close();
		}
	}
	
	public BootstrapServer() {
		activeServents = new ArrayList<>();
	}
	
	public void doBootstrap(String bsIp, int bsPort) {
		Thread cliThread = new Thread(new CLIWorker());
		cliThread.start();
		
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(bsPort, 5, InetAddress.getByName(bsIp));
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e1) {
			Logger.timestampedErrorPrint("Problem while opening listener socket.");
			System.exit(0);
		}
		
		Random rand = new Random(System.currentTimeMillis());
		
		while (working) {
			try {
				Socket newServentSocket = listenerSocket.accept();
				
				 /* 
				 * Handling these messages is intentionally sequential, to avoid problems with
				 * concurrent initial starts.
				 * 
				 * In practice, we would have an always-active backbone of servents to avoid this problem.
				 */
				
				Scanner socketScanner = new Scanner(newServentSocket.getInputStream());
				String message = socketScanner.nextLine();
				
				/*
				 * New servent has hailed us. He is sending us his own listener port.
				 * He wants to get a listener port from a random active servent,
				 * or -1 if he is the first one.
				 */
				if (message.equals("Hail")) {
					String newServentIp = socketScanner.nextLine();
					int newServentPort = socketScanner.nextInt();
					
					System.out.println("got " + newServentIp + ":" + newServentPort);
					PrintWriter socketWriter = new PrintWriter(newServentSocket.getOutputStream());
					
					if (activeServents.size() == 0) {
						socketWriter.write(String.valueOf(-1) + "\n");
						activeServents.add(new ServentLocation(newServentIp, newServentPort)); //first one doesn't need to confirm
					} else {
//						ServentLocation randServent = activeServents.get(rand.nextInt(activeServents.size()));
						ServentLocation randServent = activeServents.get(0);
						socketWriter.write(String.valueOf(randServent.getPort()) + "\n");
						socketWriter.write(randServent.getIp() + "\n");
					}
					
					socketWriter.flush();
					newServentSocket.close();
				} else if (message.equals("New")) {
					/**
					 * When a servent is confirmed not to be a collider, we add him to the list.
					 */
					String newServentIp = socketScanner.nextLine();
					int newServentPort = socketScanner.nextInt();
					
					System.out.println("adding " + newServentIp + ":" + newServentPort);
					
					activeServents.add(new ServentLocation(newServentIp, newServentPort));
					newServentSocket.close();
				} else if (message.equals("Bye")) {
					/**
					 * When a servent is confirmed not to be a collider, we add him to the list.
					 */
					String oldServentIp = socketScanner.nextLine();
					int oldServentPort = socketScanner.nextInt();

					System.out.println("removing " + oldServentIp + ":" + oldServentPort);

					activeServents.removeIf(serventLocation -> {
						return serventLocation.getPort() == oldServentPort && serventLocation.getIp().equals(oldServentIp);
					});
					newServentSocket.close();
				}

			} catch (SocketTimeoutException e) {
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Expects one command line argument - the port to listen on.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			Logger.timestampedErrorPrint("Bootstrap started without port argument.");
		}
		
		int bsPort = 0;
		try {
			bsPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			Logger.timestampedErrorPrint("Bootstrap port not valid: " + args[0]);
			System.exit(0);
		}

		Logger.timestampedStandardPrint("Bootstrap server started on port: " + bsPort);
		
		BootstrapServer bs = new BootstrapServer();
		bs.doBootstrap("localhost", bsPort);
	}
}
