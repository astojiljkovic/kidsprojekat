package servent.message.util;

import servent.handler.ResponseMessageHandler;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

class SentMessage {
    private final int messageId;

    private final ResponseMessageHandler responseMessageHandler;
    private final MessageUtil.MessageTimeoutHandler messageTimeoutHandler;

    private boolean messageTimedOut = false; //timeoutHandlerExecuted
    private boolean messageExecuted = false; //messageHandlerExecuted

    private final Timer timer = new Timer();

    private final Object lock = new Object();

    public Optional<ResponseMessageHandler> getMessageHandlerForExecution() {
        synchronized (lock) {
            messageExecuted = true;
            timer.cancel();
            if (messageTimedOut) {
                return Optional.empty();
            } else {
                return Optional.of(responseMessageHandler);
            }
        }
    }

    public SentMessage(int messageId, ResponseMessageHandler responseMessageHandler) {
        this.messageId = messageId;
        this.responseMessageHandler = responseMessageHandler;
        this.messageTimeoutHandler = null;
    }

    public SentMessage(int messageId, ResponseMessageHandler responseMessageHandler, long timeout, MessageUtil.MessageTimeoutHandler messageTimeoutHandler) {
        this.messageId = messageId;
        this.responseMessageHandler = responseMessageHandler;
        this.messageTimeoutHandler = messageTimeoutHandler;
        if (timeout > 0) {
            scheduleTimerTask(timeout, 0);
        }
    }

    private void scheduleTimerTask(long timeout, int invocation) {
        synchronized (lock) {

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    Thread.currentThread().setName("SENT message timer");
                    System.out.println("sent timer");
                    synchronized (lock) {
                        System.out.println("sent timer");
                        if (messageExecuted) {
                            return;
                        }
                        System.out.println("sent timer");
                        long extend = messageTimeoutHandler.execute(invocation);
                        if (extend == -1) {
                            System.out.println("sent timer");
                            messageTimedOut = true;
                            timer.cancel();
                        } else {
                            System.out.println("sent timer");
                            scheduleTimerTask(extend, invocation + 1);
                        }
                    }
                }
            };

            timer.schedule(task, timeout);
        }
    }

    public int getMessageId() {
        return messageId;
    }
}
