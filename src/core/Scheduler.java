package core;

import quantum.QuantumPolicy;
import entities.ProcessData;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Scheduler {
    private Clock clock;
    private List<CpuCore> cores;
    private final QuantumPolicy quantumPolicy;

    private List<ProcessData> processesList;
    private List<ProcessData> blockedProcessesList;
    private List<ProcessData> readyProcessesList;

    private final ConcurrentLinkedQueue<ProcessData> incoming = new ConcurrentLinkedQueue<>();
    private final AtomicInteger nextPidNumber = new AtomicInteger(1); // inicia no construtor


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

        int max = 0;
        for (ProcessData p : processesList) {
            String id = p.getID();
            int n = extractTrailingNumber(id);
            if (n > max) max = n;
            cpuLeft.put(p.getID(), p.getExecuteTime());
        }
        nextPidNumber.set(max + 1);
    }

    public void submit(int entryTime, int executeTime, boolean blocked, int blockedTime) {
        String id = "P" + nextPidNumber.getAndIncrement();
        ProcessData p = new ProcessData(id, entryTime, executeTime, 0, "PRONTO", blocked, blockedTime);
        incoming.add(p);
    }


    public void run() {
        while (!allDone()) {
            int t = clock.getGlobalTime();

            drainIncomingQueue(t);

            injectArrivals(t);     // input -> ready
            tickBlocked(t);        // atualiza bloqueios e move quem terminou para ready
            assignIdleCores();     // ready -> cores ociosos (aplica CS + quantum)
            tickCores();           // executa 1 tick em cada core (CS/execução/preempção/finish)

            clock.advance();       // avança o relógio (tempo total inclui CS e ociosidade)

        }
        printGantt();
    }

    // drenar a fila:
    private void drainIncomingQueue(int tNow) {
        boolean gotAny = false;
        for (ProcessData p; (p = incoming.poll()) != null; ) {
            gotAny = true;
            processesList.add(p);
            cpuLeft.put(p.getID(), p.getExecuteTime());
            if (!arrivedProcesses.contains(p.getID()) && p.getEntryTime() <= tNow) {
                arrivedProcesses.add(p.getID());
                readyProcessesList.add(p);
            }
        }
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

    private int extractTrailingNumber(String id) {
        if (id == null) return 0;
        int n = 0, i = id.length()-1, base = 1;
        while (i >= 0 && Character.isDigit(id.charAt(i))) {
            n += (id.charAt(i) - '0') * base; base *= 10; i--;
        }
        return n;
    }
}
