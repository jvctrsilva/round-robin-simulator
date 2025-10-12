import quantum.QuantumPolicy;
import core.Clock;
import core.Scheduler;
import entities.ProcessData;
import readArchive.ReadProcess;
import ui.Menu;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Perguntas de configuração
        Menu.Config cfg = Menu.show();

        // Leitura dos processos
        List<ProcessData> procs = ReadProcess.readProcess(cfg.filePath);

        // Relógio com tick, CS e quantum base (o valor será usado na política)
        Clock clock = new Clock(0, cfg.tick, cfg.contextSwitchCost, cfg.baseQuantum);

        // Política de quantum (fixa ou dinâmica) conforme escolha do usuário
        QuantumPolicy policy = cfg.buildPolicy();

        // Scheduler multi-core
        Scheduler scheduler = new Scheduler(clock, procs, cfg.numCores, policy);
        scheduler.run();

        System.out.println("Tempo total simulado: " + clock.getGlobalTime());
        System.out.println("OK");
    }
}
