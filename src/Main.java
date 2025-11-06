import quantum.QuantumPolicy;
import core.Clock;
import core.Scheduler;
import entities.ProcessData;
import readArchive.ReadProcess;
import server.Server;
import ui.Menu;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1) Inicia o servidor
        Server server = new Server(12345);
        server.start();

        // 2) Solicita entrada do usuário (processos adicionais) e salva no processos.txt
        final String baseFile = "src/readArchive/processos.txt";
        List<ProcessData> base = ReadProcess.readProcess(baseFile);
        int nextPid = computeNextPidNumber(base);

        // coleta no console
        collectAndAppendExtras(baseFile, nextPid);

        // 3) Solicita o Menu com os parâmetros
        Menu.Config cfg = Menu.show();

        // 4) Recarrega processos (agora já com os extras appendados)
        List<ProcessData> procs = ReadProcess.readProcess(baseFile);

        // 5) Cria clock/política/scheduler
        Clock clock = new Clock(0, cfg.tick, cfg.contextSwitchCost, cfg.baseQuantum);
        QuantumPolicy policy = cfg.buildPolicy();
        Scheduler scheduler = new Scheduler(clock, procs, cfg.numCores, policy);

        // Conecta o servidor ao scheduler
        server.setScheduler(scheduler);

        // 6) Envia para o Scheduler e roda
        scheduler.run();

        // 7) Finaliza servidor e imprime resumo
        server.close();
        System.out.println("Tempo total simulado: " + clock.getGlobalTime());
    }

    // ===== Helpers =====

    private static void collectAndAppendExtras(String filePath, int nextPid) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Deseja inserir processos adicionais agora? (s/n): ");
        String ans = sc.nextLine().trim().toLowerCase();
        boolean add = ans.equals("s") || ans.equals("sim") || ans.equals("y") || ans.equals("yes");
        if (!add) return;

        int qtd = readInt(sc, "Quantos processos deseja inserir?: ", 1, 1, 1000);

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= qtd; i++) {
            System.out.println("Processo extra #" + i);
            int entryTime   = readInt(sc, "  entryTime (>=0): ", 0, 0, Integer.MAX_VALUE);
            int executeTime = readInt(sc, "  executeTime (>=1): ", 1, 1, Integer.MAX_VALUE);
            boolean blocked = readBool(sc, "  blocked? (s/n): ", false);
            int blockedTime = blocked ? readInt(sc, "  blockedTime (>=1): ", 1, 1, Integer.MAX_VALUE) : 0;

            String pid = "P" + (nextPid++);
            // Formato do arquivo base (7 colunas):
            // id|entryTime|executeTime|remainingQuantum|state|blocked?|blockedTime
            String line = String.join("|",
                    pid,
                    String.valueOf(entryTime),
                    String.valueOf(executeTime),
                    "0",
                    "PRONTO",
                    String.valueOf(blocked),
                    String.valueOf(blockedTime)
            );
            sb.append(line).append(System.lineSeparator());
        }

        try {
            Files.write(Paths.get(filePath), sb.toString().getBytes(), StandardOpenOption.APPEND);
            System.out.println("-> " + qtd + " processo(s) gravado(s) em " + filePath);
        } catch (IOException e) {
            System.err.println("Falha ao gravar processos: " + e.getMessage());
        }
    }

    private static int computeNextPidNumber(List<ProcessData> list) {
        int max = 0;
        for (ProcessData p : list) {
            String id = p.getID();
            int num = extractTrailingNumber(id);
            if (num > max) max = num;
        }
        return max + 1;
    }

    private static int extractTrailingNumber(String id) {
        if (id == null) return 0;
        int n = 0, i = id.length() - 1, base = 1;
        while (i >= 0 && Character.isDigit(id.charAt(i))) {
            n += (id.charAt(i) - '0') * base; base *= 10; i--;
        }
        return n;
    }

    private static int readInt(Scanner sc, String prompt, int def, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (line.isEmpty()) return def;
            try {
                int v = Integer.parseInt(line);
                if (v < min || v > max) {
                    System.out.println("Valor inválido. Deve estar entre " + min + " e " + max + ".");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Número inválido. Tente novamente.");
            }
        }
    }

    private static boolean readBool(Scanner sc, String prompt, boolean def) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim().toLowerCase();
            if (line.isEmpty()) return def;
            if (line.equals("s") || line.equals("sim") || line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("nao") || line.equals("não") || line.equals("no")) return false;
            System.out.println("Responda com s/n.");
        }
    }
}
