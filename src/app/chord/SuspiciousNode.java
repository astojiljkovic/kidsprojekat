package app.chord;

import app.ServentInfo;

import java.util.Objects;

public class SuspiciousNode {
    public enum State {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuspiciousNode that = (SuspiciousNode) o;
        return Objects.equals(serventInfo, that.serventInfo) && state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serventInfo, state);
    }
}
