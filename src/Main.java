import Quantum.DynamicQuantum;
import Quantum.FixedQuantum;
import Quantum.QuantumPolicy;
import core.Clock;
import core.Scheduler;
import entities.ProcessData;
import readArchive.ReadProcess;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String path = "src/readArchive/processos.txt";
        List<ProcessData> procs = ReadProcess.readProcess(path);

        Clock clock = new Clock(0, 1, 1, 3);

        // Quantum Fixo
        QuantumPolicy policy = new FixedQuantum(clock.getQuantum());
        // Quantum dinâmico
//         QuantumPolicy policy = new DynamicQuantum(clock.getQuantum());

        int numCores = 1; // múltiplos núcleos
        Scheduler scheduler = new Scheduler(clock, procs, numCores, policy);
        scheduler.run();

        System.out.println("Tempo total simulado: " + clock.getGlobalTime());
        System.out.println("OK");
    }
}
