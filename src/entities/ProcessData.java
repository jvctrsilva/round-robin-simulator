package entities;

public class ProcessData {
    private String ID;
    private int entryTime;
    private int executeTime;
    private int quantumRemaining;
    private String state;
    private Boolean blocked;
    private int blockedTime;


    public ProcessData(String ID, int entryTime, int executeTime, int quantumRemaining, String state, Boolean blocked, int blockedTime) {
        this.ID = ID;
        this.entryTime = entryTime;
        this.executeTime = executeTime;
        this.quantumRemaining = quantumRemaining;
        this.state = state;
        this.blocked = blocked;
        this.blockedTime = blockedTime;
    }

    public Boolean getBlocked() {return blocked;}

    public int getBlockedTime() {return blockedTime;}

    public int getEntryTime() {return entryTime;}

    public int getExecuteTime() {return executeTime;}

    public String getID() {return ID;}

    public int getQuantumRemaining() {return quantumRemaining;}

    public String getState() {return state;}

    @Override
    public String toString(){
        return ID + " Chegada: " + entryTime + ", Tempo Execução: " + executeTime + ", Quantum Restante: "
        + quantumRemaining + ", Estado: " + state + ", Bloqueado?: " + blocked + ", Tempo Bloqueado: " + blockedTime;
    }
}

