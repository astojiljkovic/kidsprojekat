package servent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.Logger;
import servent.handler.*;
import servent.handler.chord.leave.LeaveGrantedHandler;
import servent.handler.chord.leave.LeaveRequestHandler;
import servent.handler.chord.leave.SuccLeavingHandler;
import servent.handler.data.AddHandler;
import servent.handler.data.CommitHandler;
import servent.handler.data.PullHandler;
import servent.handler.data.RemoveHandler;
import servent.message.Message;
import servent.message.ResponseMessage;
import servent.message.TrackedMessage;
import servent.message.chord.stabilize.PongMessage;
import servent.message.util.MessageUtil;

import javax.sound.midi.Track;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	public SimpleServentListener() {
		
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getNetworkLocation().getPort(), 100, InetAddress.getByName(AppConfig.myServentInfo.getNetworkLocation().getIp()));
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			Logger.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getNetworkLocation().getPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				Socket clientSocket = listenerSocket.accept();
				
				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);

				MessageHandler messageHandler = new NullHandler(clientMessage);

				if (clientMessage instanceof ResponseMessage) {
					ResponseMessage trackedMessage = (ResponseMessage) clientMessage;

					Optional<ResponseMessageHandler> handler = MessageUtil.removeHandlerForId(trackedMessage.getInitialId());
					if(handler.isPresent()) {
						handler.get().setMessage(trackedMessage);
						messageHandler = handler.get();
					} else {
						Logger.timestampedStandardPrint("Handler picked message that was already timedOut " + clientMessage);
						continue;
					}
				}
				else {

					/*
					 * Each message type has it's own handler.
					 * If we can get away with stateless handlers, we will,
					 * because that way is much simpler and less error prone.
					 */
					switch (clientMessage.getMessageType()) {
						case NEW_NODE:
							messageHandler = new NewNodeHandler(clientMessage);
							break;
						case WELCOME:
							messageHandler = new WelcomeHandler(clientMessage);
							break;
						case SORRY:
							messageHandler = new SorryHandler(clientMessage);
							break;
						case UPDATE:
							messageHandler = new UpdateHandler(clientMessage);
							break;
						case ADD:
							messageHandler = new AddHandler(clientMessage);
							break;
						case PULL:
							messageHandler = new PullHandler(clientMessage);
							break;
						case REMOVE:
							messageHandler = new RemoveHandler(clientMessage);
							break;
						case COMMIT:
							messageHandler = new CommitHandler(clientMessage);
							break;
						case LEAVE_REQUEST:
							messageHandler = new LeaveRequestHandler(clientMessage);
							break;
						case SUCC_LEAVING:
							messageHandler = new SuccLeavingHandler(clientMessage);
							break;
						case LEAVE_GRANTED:
							messageHandler = new LeaveGrantedHandler(clientMessage);
							break;
						case PING:
							messageHandler = new MessageHandler() {
								@Override
								public void run() {
									PongMessage pm = new PongMessage(AppConfig.myServentInfo, clientMessage.getSender());
									pm.copyContextFrom((TrackedMessage) clientMessage);
									MessageUtil.sendAndForgetMessage(pm);
								}
							};
						case POISON:
							break;
					}
				}

				threadPool.execute(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
