package app.chord;

import app.ServentInfo;

public class SuspiciousNode {
    enum State {
        SOFT_DEAD, DEAD
    }

    private final ServentInfo serventInfo;
    private final State state;

    public SuspiciousNode(ServentInfo serventInfo, State state) {
        this.serventInfo = serventInfo;
        this.state = state;
    }

    public ServentInfo getServentInfo() {
        return serventInfo;
    }

    public State getState() {
        return state;
    }
}
