package core;

class SystemStats {
    long busyTicks = 0;     // CPU útil
    long idleTicks = 0;     // ocioso
    long csTicks = 0;       // ticks rotulados "CS"
    long dispatchEvents = 0;// despachos (start) — inclui idle->proc

    long sumAssignedQuantum = 0;
    long countAssignedQuantum = 0;

    long makespan = 0;
    int finishedCount = 0;
}