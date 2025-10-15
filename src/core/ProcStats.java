package core;

class ProcStats {
    final String pid;
    final int arrival;          // Ai
    Integer firstStart = null;  // para Ri
    Integer completion = null;  // Ci
    int service = 0;            // Si (ticks úteis)
    int readyWaiting = 0;       // Wi (ticks em READY)
    int ioBlocked = 0;          // ticks em BLOQUEADO (opcional)
    int dispatches = 0;         // vezes que entrou em CPU

    ProcStats(String pid, int arrival) {
        this.pid = pid;
        this.arrival = arrival;
    }
}
