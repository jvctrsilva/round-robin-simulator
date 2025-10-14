package Quantum;

public interface QuantumPolicy {
    int nextQuantum(int readySize, int runningCores);
}