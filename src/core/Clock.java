package core;

public class Clock {
    private int globalTime;
    private int tick;
    private int contextSwitch;
    private int quantum;

    public Clock(int globalTime, int tick, int contextSwitch, int quantum) {
        this.globalTime = globalTime;
        this.tick = tick;
        this.contextSwitch = contextSwitch;
        this.quantum = quantum;
    }

    public int getContextSwitch() {return contextSwitch;}

    public int getGlobalTime() {return globalTime;}

    public int getQuantum() {return quantum;}

    public int getTick() {return tick;}

    public void advance() { globalTime += tick; }
}
