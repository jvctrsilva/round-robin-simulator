package server;

import java.io.Serializable;

public class ProcessRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    public int entryTime;     // quando “chega” ao sistema
    public int executeTime;   // tempo total de CPU
    public boolean blocked;   // bloqueia ao fim do quantum?
    public int blockedTime;   // duração do bloqueio, se blocked=true

    public ProcessRequest(int entryTime, int executeTime, boolean blocked, int blockedTime) {
        this.entryTime = entryTime;
        this.executeTime = executeTime;
        this.blocked = blocked;
        this.blockedTime = blockedTime;
    }

    @Override public String toString() {
        return "ProcessRequest{entry=" + entryTime + ", exec=" + executeTime +
                ", blocked=" + blocked + ", blockedTime=" + blockedTime + "}";
    }
}
