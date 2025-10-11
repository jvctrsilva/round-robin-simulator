package core;

import entities.ProcessData;

import java.util.*;

public class Scheduler {
    private Clock clock;
    private List<CpuCore> cores;

    private List<ProcessData> processesList;
    private List<ProcessData> blockedProcessesList;
    private List<ProcessData> readyProcessesList;

    //Auxiliares
    private final Map<String, Integer> cpuLeft = new HashMap<>(); // CPU restante por processo
    private final Set<String> arrivedProcesses = new HashSet<>();

    public Scheduler(Clock clock, List<ProcessData> processes, int numCores){
        this.clock = clock;
        this.processesList = processes;
        this.cores = new ArrayList<>();
        for(int i = 0; i < numCores; i++){
            cores.add(new CpuCore(i));
        }
        this.readyProcessesList   = new ArrayList<>();
        this.blockedProcessesList = new ArrayList<>();

        for (ProcessData p : processesList) {
            cpuLeft.put(p.getID(), p.getExecuteTime());
        }
    }


    // Conta as chegadas (Processo chegou)
    private void isArrived (int t){
        for(ProcessData p : processesList){
            if(!arrivedProcesses.contains(p.getID()) & p.getEntryTime() == t){
                arrivedProcesses.add(p.getID());
                readyProcessesList.add(p);
            }
        }
    }
}
