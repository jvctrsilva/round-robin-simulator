package quantum;

public class FixedQuantum  implements QuantumPolicy {

    private final int q;
    public FixedQuantum(int q){
        this.q = q;
    }
    public int nextQuantum(int readySize, int runningCores){ return q; }
}
