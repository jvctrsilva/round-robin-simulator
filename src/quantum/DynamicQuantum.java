package quantum;

public class DynamicQuantum implements QuantumPolicy {
    private final int base;
    private final int minQ;
    private final int maxQ;

    public DynamicQuantum(int base) {
        this.base = base;
        this.minQ = 1;
        this.maxQ = base * 2;
    }

    // Mais carga -> quantum menor; Menos carga -> quantum maior
    public int nextQuantum(int readySize, int runningCores) {
        int load = readySize + runningCores; // Processos ativos (esperando) + rodando
        if (load <= runningCores) return maxQ; // Sobrando. Aumentar quantum;
        if (load <= runningCores * 2) return base; // Carga moderada
        return base/2; // Faltando. Diminuir quantum
    }
}
