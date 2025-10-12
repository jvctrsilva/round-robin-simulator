import core.Clock;
import core.Scheduler;
import entities.ProcessData;
import readArchive.ReadProcess;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String path = "src/readArchive/processos.txt";
        List<ProcessData> procs = ReadProcess.readProcess(path);

        Clock clock = new Clock(0, 1, 1, 4);

        int numCores = 2; // múltiplos núcleos
        Scheduler scheduler = new Scheduler(clock, procs, numCores);
        scheduler.run();

        System.out.println("Tempo total simulado: " + clock.getGlobalTime());
        System.out.println("OK");
    }
}
