package core;

import Quantum.QuantumPolicy;
import entities.ProcessData;

import java.util.*;

public class Scheduler {
    private Clock clock;
    private List<CpuCore> cores;
    private final QuantumPolicy quantumPolicy;

    private List<ProcessData> processesList;
    private List<ProcessData> blockedProcessesList;
    private List<ProcessData> readyProcessesList;

    //Atributos auxiliares
    private final Map<String, Integer> cpuLeft = new HashMap<>();   // CPU restante por processo
    private final Set<String> arrivedProcesses = new HashSet<>();   // Processos que chegaram
    private final Set<String> finished = new HashSet<>();           // Processos finalizados
    private final Map<String, Integer> blockLeft = new HashMap<>(); // Bloqueados restantes

    private final List<List<String>> gantt = new ArrayList<>();

    public Scheduler(Clock clock, List<ProcessData> processes, int numCores, QuantumPolicy policy) {
        this.clock = clock;
        this.processesList = processes;
        this.cores = new ArrayList<>();
        for(int i = 0; i < numCores; i++){
            cores.add(new CpuCore(i));
            gantt.add(new ArrayList<>()); // cria a linha para cada core
        }
        this.quantumPolicy = policy;
        this.readyProcessesList   = new ArrayList<>();
        this.blockedProcessesList = new ArrayList<>();

        for (ProcessData p : processesList) {
            cpuLeft.put(p.getID(), p.getExecuteTime());
        }
    }

    public void run() {
        while (!allDone()) {
            int t = clock.getGlobalTime();

            injectArrivals(t);     // input -> ready
            tickBlocked(t);        // atualiza bloqueios e move quem terminou para ready
            assignIdleCores();     // ready -> cores ociosos (aplica CS + quantum)
            tickCores();           // executa 1 tick em cada core (CS/execução/preempção/finish)

            clock.advance();       // avança o relógio (tempo total inclui CS e ociosidade)
        }
        printGantt();
    }

    private boolean allDone() {
        return finished.size() == processesList.size();
    }

    // Conta chegadas no tempo t e move para ready
    private void injectArrivals(int t){
        for (ProcessData p : processesList) {
            if (!arrivedProcesses.contains(p.getID()) && p.getEntryTime() == t) {
                arrivedProcesses.add(p.getID());
                readyProcessesList.add(p);
            }
        }
    }

    // Se houver CPU livre e processos prontos, inicia execução aplicando CS e quantum
    private void assignIdleCores() {
        Iterator<ProcessData> it = readyProcessesList.iterator();
        int running = 0;
        for (CpuCore c: cores) if (!c.isIdle()) running++; // Quantidade de cores que estão ativas

        for (CpuCore core : cores) {
            if (!core.isIdle()) continue;
            if (!it.hasNext()) break;

            ProcessData next = it.next();
            it.remove();

            int q = quantumPolicy.nextQuantum(readyProcessesList.size(), running);
            core.start(next.getID(), q, clock.getContextSwitchCost());
            running++;
        }
    }

    // Executa 1 tick em cada core; finaliza, preempta e realoca quando necessário
    private void tickCores() {
        for (CpuCore core : cores) {
            String mark = core.tick(); // "CS", "-", ou ID
            gantt.get(core.getId()).add(mark); // registra no Gantt deste core

            if ("CS".equals(mark) || "-".equals(mark)) {
                // apenas consumiu troca de contexto ou está ocioso — nada a fazer
                continue;
            }

            // Está executando 1 tick do processo 'pid'
            String pid = mark;

            // Consome 1 de CPU restante
            int left = cpuLeft.get(pid) - 1;
            cpuLeft.put(pid, left);

            // 1) Terminou?
            if (left <= 0) {
                finished.add(pid);
                core.free();
                continue;
            }

            // 2) Quantum acabou? Preempção — devolve para PRONTO e libera o core
            // 2) Quantum zerou?
            if (core.getQuantumRemaining() == 0) {
                ProcessData p = findById(pid);

                // Se o processo é "bloqueável" e tem tempo de bloqueio, entra em BLOQUEADO
                if (p.getBlocked() && p.getBlockedTime() > 0) {
                    core.free();
                    blockedProcessesList.add(p);
                    blockLeft.put(pid, Math.max(1, p.getBlockedTime()));
                } else {
                    // Caso contrário, preempção normal: volta para PRONTO
                    readyProcessesList.add(p);
                    core.free();
                }
            }
        }

        // Depois das liberações/preempções, tentar alocar novamente ainda neste tick
        assignIdleCores();
    }

    private void tickBlocked(int t) {
        if (blockedProcessesList.isEmpty()) return;

        List<ProcessData> toReady = new ArrayList<>();
        for (ProcessData p : blockedProcessesList) {
            String id = p.getID();
            int left = blockLeft.getOrDefault(id, 0);
            if (left > 0) {
                left -= 1;
                blockLeft.put(id, left);
            }
            if (left == 0) {
                toReady.add(p);
            }
        }
        // mover quem terminou o bloqueio para PRONTO
        for (ProcessData p : toReady) {
            blockedProcessesList.remove(p);
            blockLeft.remove(p.getID());
            readyProcessesList.add(p);
        }
    }
    private void printGantt() {
        System.out.println("\n=== GANTT ===");
        for (int c = 0; c < gantt.size(); c++) {
            System.out.print("Core " + c + ": ");
            for (String s : gantt.get(c)) System.out.print("| " + s + " ");
            System.out.println("|");
        }
        // linha do tempo (assume pelo menos 1 core)
        if (!gantt.isEmpty()) {
            System.out.print("Tempo : ");
            for (int t = 0; t < gantt.get(0).size(); t++) System.out.print(t + "   ");
            System.out.println();
        }
    }

    // ===== Métodos auxiliares =====
    private ProcessData findById(String id) {
        for (ProcessData p : processesList) if (p.getID().equals(id)) return p;
        throw new IllegalStateException("Processo não encontrado: " + id);
    }
}
