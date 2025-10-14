package core;

import quantum.QuantumPolicy;
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
    private final Map<String, ProcStats> stats = new HashMap<>();
    private final SystemStats sys = new SystemStats();

    // para ajudar a detectar despachos e sequência de CS
    private final Map<Integer, String> lastMarkPerCore = new HashMap<>();
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

        for (ProcessData p : processesList) {
            cpuLeft.put(p.getID(), p.getExecuteTime());
            stats.put(p.getID(), new ProcStats(p.getID(), p.getEntryTime()));
        }

        for (CpuCore c : cores) {
            lastMarkPerCore.put(c.getId(), "--"); // começa ocioso
        }
    }

    public void run() {
        while (!allDone()) {
            int t = clock.getGlobalTime();

            injectArrivals(t);     // input -> ready
            tickBlocked(t);        // atualiza bloqueios e move quem terminou para ready
            assignIdleCores();     // ready -> cores ociosos (aplica CS + quantum)

            for (ProcessData p : readyProcessesList) {
                ProcStats s = stats.get(p.getID());
                if (s != null) s.readyWaiting++;
            }

            tickCores();           // executa 1 tick em cada core (CS/execução/preempção/finish)

            clock.advance();       // assume tick=1; se não for, as métricas viram "unidades de tick"
        }

        sys.makespan = clock.getGlobalTime();
        sys.finishedCount = finished.size();
        printGantt();
        writeReportFiles(); // <=== novo
    }

    private boolean allDone() {
        return finished.size() == processesList.size();
    }

    // Conta chegadas no tempo t e move para ready
    private void injectArrivals(int t){
        for (ProcessData p : processesList) {
            if (!arrivedProcesses.contains(p.getID()) && p.getEntryTime() == t) {
                arrivedProcesses.add(p.getID());
                p.setState("PRONTO");
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
            System.out.println(readyProcessesList.size());
            int q = quantumPolicy.nextQuantum(readyProcessesList.size(), running);
            sys.sumAssignedQuantum += q;
            sys.countAssignedQuantum++;

            // contamos um despacho (start). Se quiser contar só troca real (não idle->proc),
            // cheque lastMarkPerCore.get(core.getId()) e incremente apenas se for diferente de "--".
            sys.dispatchEvents++;
            core.start(next.getID(), q, clock.getContextSwitchCost());
            running++;
        }
    }

    // Executa 1 tick em cada core; finaliza, preempta e realoca quando necessário
    private void tickCores() {
        for (CpuCore core : cores) {
            String mark = core.tick(); // "CS", "-", ou ID
            gantt.get(core.getId()).add(mark); // registra no Gantt deste core

            if ("CS".equals(mark)) {
                sys.csTicks++;
                lastMarkPerCore.put(core.getId(), "CS");
                continue;
            }
            if ("--".equals(mark)) {
                sys.idleTicks++;
                lastMarkPerCore.put(core.getId(), "--");
                continue;
            }


            // Executando PID
            sys.busyTicks++;
            String pid = mark;
            ProcessData pNow = findById(pid);
            pNow.setState("EXECUÇÃO");

            // stats do processo
            ProcStats s = stats.get(pid);
            if (s != null) {
                s.service++;

                String prev = lastMarkPerCore.get(core.getId()); // o que estava no core no tick anterior
                boolean isNewDispatch = (s.firstStart == null) || (prev == null) || !pid.equals(prev);
                if (isNewDispatch) {
                    if (s.firstStart == null) {
                        s.firstStart = clock.getGlobalTime(); // primeira vez que pega CPU
                    }
                    s.dispatches++;
                }
            }

            // Consome 1 de CPU restante (protege contra NPE)
            Integer cur = cpuLeft.get(pid);
            int left = (cur == null ? 0 : cur) - 1;
            cpuLeft.put(pid, left);

            // Terminou?
            if (left <= 0) {
                finished.add(pid);
                pNow.setState("FINALIZADO");
                if (s != null && s.completion == null) {
                    s.completion = clock.getGlobalTime() + 1; // termina ao fim do tick
                }
                core.free();
                lastMarkPerCore.put(core.getId(), pid);
                continue;
            }


            // Quantum zerou?
            if (core.getQuantumRemaining() == 0) {
                ProcessData p = findById(pid);
                core.free();
                if (p.getBlocked() && p.getBlockedTime() > 0) {
                    p.setState("BLOQUEADO");
                    blockedProcessesList.add(p);
                    blockLeft.put(pid, Math.max(1, p.getBlockedTime()));
                } else {
                    readyProcessesList.add(p);
                }
                lastMarkPerCore.put(core.getId(), pid);
            } else {
                lastMarkPerCore.put(core.getId(), pid);
            }
        }

        // Depois das liberações/preempções, tentar alocar novamente ainda neste tick
        //assignIdleCores();
    }

    private void tickBlocked(int t) {
        if (blockedProcessesList.isEmpty()) return;

        List<ProcessData> toReady = new ArrayList<>();
        for (ProcessData p : blockedProcessesList) {


            String id = p.getID();
            int left = blockLeft.getOrDefault(id, 0);
            if (left == 0) {
                toReady.add(p);
            }

            if (left > 0) {
                left -= 1;
                blockLeft.put(id, left);
                ProcStats s = stats.get(p.getID());
                if (s != null) s.ioBlocked++; // conta 1 tick de IO
            }

        }
        // mover quem terminou o bloqueio para PRONTO
        for (ProcessData p : toReady) {
            blockedProcessesList.remove(p);
            blockLeft.remove(p.getID());
            p.setState("PRONTO");
            readyProcessesList.add(p);
        }
    }


    // ===== Métodos auxiliares =====
    private ProcessData findById(String id) {
        for (ProcessData p : processesList) if (p.getID().equals(id)) return p;
        throw new IllegalStateException("Processo não encontrado: " + id);
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
            System.out.print("Tempo: " + " ");
            for (int t = 0; t < gantt.get(0).size(); t++) System.out.print((t < 10 ? "0" + t : String.valueOf(t)) + "   ");
            System.out.print(clock.getGlobalTime());
            System.out.println();
        }
    }

    private void writeReportFiles() {
        StringBuilder sb = new StringBuilder();
        sb.append("RELATORIO DE SIMULACAO - Round Robin\n");
        sb.append("====================================\n\n");

        sb.append("Legenda das colunas:\n");
        sb.append("PID: identificador do processo\n");
        sb.append("Ai: arrival (tempo de chegada)\n");
        sb.append("Ci: completion (tempo de conclusao)\n");
        sb.append("Ri: response time = (primeira entrada em CPU) - Ai\n");
        sb.append("Si: service time = ticks uteis de CPU\n");
        sb.append("Wi: waiting time = ticks em READY\n");
        sb.append("IO: ticks bloqueado em E/S\n");
        sb.append("Disp: numero de despachos (vezes que entrou em CPU)\n\n");

        sb.append(String.format("%-6s %-4s %-4s %-4s %-4s %-4s %-4s %-5s\n",
                "PID","Ai","Ci","Ri","Si","Wi","IO","Disp"));

        List<ProcessData> ordered = new ArrayList<>(processesList);
        ordered.sort(java.util.Comparator.comparing(ProcessData::getID));

        double sumR=0, sumT=0, sumW=0;
        int n = ordered.size();

        for (ProcessData p : ordered) {
            ProcStats s = stats.get(p.getID());
            if (s == null) continue;
            int Ri = (s.firstStart == null ? -1 : (s.firstStart - s.arrival));
            int Ci = (s.completion == null ? -1 : s.completion);
            int Ti = (s.completion == null ? -1 : (s.completion - s.arrival));
            int Wi = s.readyWaiting;

            if (Ri >= 0) sumR += Ri;
            if (Ti >= 0) sumT += Ti;
            sumW += Wi;

            sb.append(String.format("%-6s %-4d %-4d %-4d %-4d %-4d %-4d %-5d\n",
                    s.pid, s.arrival, Ci, Ri, s.service, Wi, s.ioBlocked, s.dispatches));
        }

        long totalTicks = sys.busyTicks + sys.idleTicks + sys.csTicks;
        double utilEfetiva = totalTicks > 0 ? (double) sys.busyTicks / totalTicks : 0.0;
        double avgQ = sys.countAssignedQuantum > 0 ? (double) sys.sumAssignedQuantum / sys.countAssignedQuantum : 0.0;

        double avgR = n > 0 ? sumR / n : 0.0;
        double avgT = n > 0 ? sumT / n : 0.0;
        double avgW = n > 0 ? sumW / n : 0.0;

        sb.append("\n=== Sumario do Sistema ===\n");
        sb.append("Makespan (tempo total): ").append(sys.makespan).append("\n");
        sb.append("Processos finalizados : ").append(sys.finishedCount).append(" / ").append(n).append("\n");
        sb.append("Busy ticks (CPU util) : ").append(sys.busyTicks).append("\n");
        sb.append("CS ticks (overhead)   : ").append(sys.csTicks).append("\n");
        sb.append("Idle ticks            : ").append(sys.idleTicks).append("\n");
        sb.append("Despachos (starts)    : ").append(sys.dispatchEvents).append("\n");
        sb.append(String.format("Utilizacao util       : %.2f%%\n", utilEfetiva * 100.0));
        sb.append(String.format("Quantum medio         : %.2f (amostras=%d)\n", avgQ, sys.countAssignedQuantum));
        sb.append(String.format("Tempo medio resposta  : %.2f\n", avgR));
        sb.append(String.format("Tempo medio espera    : %.2f\n", avgW));
        sb.append(String.format("Turnaround medio      : %.2f\n", avgT));
        sb.append("\nNotas:\n");
        sb.append("- Q pequeno => mais CS (csTicks) => menor utilizacao e maior Wi/Ti.\n");
        sb.append("- Se custo de CS ~ Q, espere eficiencia proxima de 50%% do tempo util.\n");
        sb.append("- Q dinamico ajusta Ri/Ti conforme carga e ociosidade/I-O.\n");

        try (java.io.PrintWriter out = new java.io.PrintWriter("relatorio.txt", java.nio.charset.StandardCharsets.UTF_8)) {
            out.print(sb.toString());
        } catch (Exception e) {
            System.err.println("Falha ao gravar relatorio.txt: " + e.getMessage());
        }

        // CSV por processo
        try (java.io.PrintWriter csv = new java.io.PrintWriter("relatorio.csv", java.nio.charset.StandardCharsets.UTF_8)) {
            csv.println("PID,Ai,Ci,Ri,Si,Wi,IO,Disp");
            for (ProcessData p : ordered) {
                ProcStats s = stats.get(p.getID());
                if (s == null) continue;
                int Ri = (s.firstStart == null ? -1 : (s.firstStart - s.arrival));
                int Ci = (s.completion == null ? -1 : s.completion);
                csv.printf("%s,%d,%d,%d,%d,%d,%d,%d%n",
                        s.pid, s.arrival, Ci, Ri, s.service, s.readyWaiting, s.ioBlocked, s.dispatches);
            }
        } catch (Exception e) {
            System.err.println("Falha ao gravar relatorio.csv: " + e.getMessage());
        }
    }

}
