package entities;

public class ProcessData {
    private String id;
    private int timeEnter;
    private int timeExecute;
    private int timeRemaining;
    private int quantumRemaining;
    private String state;

    public ProcessData(String id, int timeEnter, int timeExecute, int timeRemaining, int quantumRemaining, String state){
        this.id = id;
        this.timeEnter = timeEnter;
        this.timeExecute = timeExecute;
        this.timeRemaining = timeRemaining;
        this.quantumRemaining = quantumRemaining;
        this.state = state;
    }

    @Override
    public String toString(){
        return id + " Chegada: " + timeEnter + ", Tempo Execução: " + timeExecute + ", Tempo Restante: "
                + timeRemaining + ", Quantum Restante: " + quantumRemaining + ", Estado: " + state;
    }
}

