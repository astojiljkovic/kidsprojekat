package app.chord;

import app.AppConfig;
import app.Logger;
import app.ServentInfo;
import servent.handler.ResponseMessageHandler;
import servent.message.chord.stabilize.PingMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StateStabilizer {

    public interface NotAnsweringNotification {
        void nodeNotAnswering(ServentInfo node, boolean isSoftTimeout);
    }

    private List<ServentInfo> serventsToPing = new ArrayList<>();

    private final NotAnsweringNotification notificationHandler;

    public StateStabilizer(NotAnsweringNotification notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    public synchronized void stopPingingNodeAndStartPingingAnother(ServentInfo nodeToStop, ServentInfo nodeToStart) {
        serventsToPing.remove(nodeToStop);
        serventsToPing.add(nodeToStart);

        initiatePing(nodeToStart);
    }

    private synchronized void initiatePing(ServentInfo node) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                PingMessage pingMessage = new PingMessage(AppConfig.myServentInfo, node);
                MessageUtil.sendTrackedMessageAwaitingResponse(pingMessage, new ResponseMessageHandler() {
                    @Override
                    public void run() {
                        Logger.timestampedStandardPrint("Pong received " + node.getChordId());
                        initiatePing(node);
                    }
                }, 1000, invocation -> {
                    if (invocation == 0) {
                        notifyNodeNotAnswering(true, node);
                        return 9000;
                    }
                    notifyNodeNotAnswering(false, node);
                    return -1;
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 1000);
    }

    private synchronized void notifyNodeNotAnswering(boolean isSoftTimeout, ServentInfo node) {
        if(serventsToPing.contains(node)) {
            notificationHandler.nodeNotAnswering(node, isSoftTimeout);
        }
    }
}
