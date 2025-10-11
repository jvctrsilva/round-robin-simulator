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

    /** Inicia (ou recomeça) um processo no núcleo, aplicando custo de troca de contexto e quantum atual */
    public void start(String processId, int quantum, int ContextSwitchCost) {
        this.runningId = processId;
        this.quantumRemaining = quantum;
        this.contextSwitchLeft = ContextSwitchCost;
    }
    /** Libera o núcleo (sem processo) */
    public void free(){
        runningId = null;
        quantumRemaining = 0;
        // contextSwitchLeft permanece 0; novo CS será aplicado no próximo start()
    }

    /**
     * Avança 1 tick:
     *  - Se em CS: consome 1 e retorna "CS"
     *  - Se ocioso: retorna "-"
     *  - Se executando: consome 1 de quantum e retorna o ID do processo
     */
    public String tick() {
        if (contextSwitchLeft > 0) {
            contextSwitchLeft--;
            return "CS";
        }
        if (runningId == null) {
            return "-";
        }
        if (quantumRemaining > 0) quantumRemaining--;
        return runningId;
    }

}
