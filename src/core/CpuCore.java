package core;

public class CpuCore {
    private int id;
    private String runningId;
    private int quantumRemaining;
    private int contextSwitchLeft;

    private int executionTime; // Tempo de execucao total (ver qual CPU ira ser chamada)

    public CpuCore(int id) {
        this.id = id;
        this.runningId = null;
        this.quantumRemaining = 0;
        this.contextSwitchLeft = 0;
        this.executionTime = 0;
    }
    public int getId() {return id;}
    public String getRunningId() {return runningId;}
    public int getQuantumRemaining() {return quantumRemaining;}
    public int getExecutionTime() {return executionTime;}

    public boolean isIdle(){return runningId == null & quantumRemaining == 0 & contextSwitchLeft == 0;}
    public boolean inContextSwitch(){return contextSwitchLeft > 0;}


    public void start(String processId, int quantum, int ContextSwitchCost) {
        this.runningId = processId;
        this.quantumRemaining = quantumRemaining;
        this.contextSwitchLeft = contextSwitchLeft;
    }

    public void free(){
        runningId = null;
        quantumRemaining = 0;
    }


}
