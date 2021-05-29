package app.chord;

import app.AppConfig;
import app.Logger;
import app.ServentInfo;
import servent.handler.ResponseMessageHandler;
import servent.message.chord.stabilize.PingMessage;
import servent.message.util.MessageUtil;

import java.util.*;

public class StateStabilizer {

    public interface NotAnsweringNotification {
        void nodeNotAnswering(ServentInfo node, boolean isSoftTimeout);
    }

    public interface AnsweredNotification {
        void nodeAnswered(ServentInfo node);
    }

    private HashMap<ServentInfo, Timer> serventsToPing = new HashMap<>();

    private final NotAnsweringNotification notificationHandler;
    private final AnsweredNotification answeredNotificationHandler;

    public StateStabilizer(AnsweredNotification answeredNotificationHandler, NotAnsweringNotification notificationHandler) {
        this.answeredNotificationHandler = answeredNotificationHandler;
        this.notificationHandler = notificationHandler;
    }

//    public synchronized void stopPingingNodeAndStartPingingAnother(ServentInfo nodeToStop, ServentInfo nodeToStart) {
//        serventsToPing.remove(nodeToStop);
//        serventsToPing.add(nodeToStart);
//
//        initiatePing(nodeToStart);
//    }

    public synchronized void pingNodes(List<ServentInfo> newServentsToPing) {
        List<ServentInfo> toRemove = new ArrayList<>();
        for (Map.Entry<ServentInfo, Timer> entry: serventsToPing.entrySet()) {
            if(!newServentsToPing.contains(entry.getKey())) {
                entry.getValue().cancel();
                toRemove.add(entry.getKey());
            }
        }

        for(ServentInfo remove: toRemove) {
            serventsToPing.remove(remove);
        }

        for (ServentInfo newServent: newServentsToPing) {
            if (serventsToPing.containsKey(newServent)){
                continue;
            }
            TimerTask tt = timerTask(newServent);
            Timer timer = new Timer();
            timer.schedule(tt, 1000);
            serventsToPing.put(newServent, timer);
        }

        System.out.println("---Nodes to ping---");
        for (ServentInfo servent: serventsToPing.keySet()) {
            System.out.println("" + servent);
        }
    }

    private synchronized TimerTask timerTask(ServentInfo node) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                PingMessage pingMessage = new PingMessage(AppConfig.myServentInfo, node);
                MessageUtil.sendTrackedMessageAwaitingResponse(pingMessage, new ResponseMessageHandler() {
                    @Override
                    public void run() {
                        answeredNotificationHandler.nodeAnswered(node);
                        rescheduleForNode(node);
                    }
                }, 1000, invocation -> {
                    if (invocation == 0) {
                        notifyNodeNotAnswering(true, node);
                        return 9000;
                    }
                    notifyNodeNotAnswering(false, node);
                    return 10000;
                });
            }
        };

        return timerTask;
//        Timer timer = new Timer();
//        timer.schedule(timerTask, 1000);
    }

    private synchronized void rescheduleForNode(ServentInfo node) {
        Timer t = serventsToPing.get(node);
        if (t != null) {
            t.schedule(timerTask(node), 1000);
        }
    }

    private synchronized void notifyNodeNotAnswering(boolean isSoftTimeout, ServentInfo node) {
        if(serventsToPing.containsKey(node)) {
            notificationHandler.nodeNotAnswering(node, isSoftTimeout);
        }
    }
}
