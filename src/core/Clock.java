package core;

public class Clock {
    private int globalTime;
    private int tick;
    private int contextSwitchCost;
    private int quantum;

    public Clock(int globalTime, int tick, int contextSwitchCost, int quantum) {
        this.globalTime = globalTime;
        this.tick = tick;
        this.contextSwitchCost = contextSwitchCost;
        this.quantum = quantum;
    }

    public int getContextSwitchCost() {return contextSwitchCost;}

    public int getGlobalTime() {return globalTime;}

    public void advance() { globalTime += tick; }
}
