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

    private volatile boolean shouldStop = false;

    public StateStabilizer(AnsweredNotification answeredNotificationHandler, NotAnsweringNotification notificationHandler) {
        this.answeredNotificationHandler = answeredNotificationHandler;
        this.notificationHandler = notificationHandler;
    }

    public synchronized void stopStabilizer() {
        shouldStop = true;
        for(Timer t: serventsToPing.values()) {
            t.cancel();
        }
    }

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

        System.out.println("---Nodes to remove from ping---");
        for (ServentInfo servent: toRemove) {
            System.out.println("" + servent);
        }
    }

    private synchronized TimerTask timerTask(ServentInfo node) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("stabilizer timer");
                Thread.currentThread().setName("Stabilizer timer");
                if(!shouldStop) {
                    System.out.println("stabilizer timer");
                    PingMessage pingMessage = new PingMessage(AppConfig.myServentInfo, node);
                    MessageUtil.sendTrackedMessageAwaitingResponse(pingMessage, new ResponseMessageHandler() {
                        @Override
                        public void run() {
                            answeredNotificationHandler.nodeAnswered(node);
                            rescheduleForNode(node);
                        }
                    }, 1000, invocation -> {
                        System.out.println("stabilizer timer");
                        if (invocation == 0) {
                            notifyNodeNotAnswering(true, node);
                            return 9000;
                        }
                        notifyNodeNotAnswering(false, node);
                        return 10000;
                    });
                }
            }
        };

        return timerTask;
    }

    private synchronized void rescheduleForNode(ServentInfo node) {
        if (!shouldStop) {

            Timer t = serventsToPing.get(node);
            if (t != null) {
                t.schedule(timerTask(node), 1000);
            }
        }
    }

    private synchronized void notifyNodeNotAnswering(boolean isSoftTimeout, ServentInfo node) {
        if(serventsToPing.containsKey(node)) {
            notificationHandler.nodeNotAnswering(node, isSoftTimeout);
        }
    }
}
