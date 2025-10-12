package ui;

import java.util.Scanner;

import Quantum.DynamicQuantum;
import Quantum.FixedQuantum;
import Quantum.QuantumPolicy;

public class Menu {

    public static class Config {
        public String filePath;
        public int tick;
        public int contextSwitchCost;
        public int baseQuantum;
        public boolean dynamic;
        public int numCores;

        public QuantumPolicy buildPolicy() {
            if (dynamic) return new DynamicQuantum(baseQuantum);
            return new FixedQuantum(baseQuantum);
        }
    }

    public static Config show() {
        Scanner sc = new Scanner(System.in);
        Config cfg = new Config();

        System.out.println("==== Round Robin Simulator ====");
        // Caminho do arquivo: hardcoded (padrão)
        cfg.filePath = "src/readArchive/processos.txt";

        // Núcleos
        cfg.numCores = readInt(sc, "Núcleos de CPU (>=2): ", 2, 2, Integer.MAX_VALUE);

        // Tick
        cfg.tick = readInt(sc, "Tick global (>=1): ", 1, 1, Integer.MAX_VALUE);

        // Custo de troca de contexto
        cfg.contextSwitchCost = readInt(sc, "Custo de Context Switch (>=0): ", 1, 0, Integer.MAX_VALUE);

        // Quantum base
        cfg.baseQuantum = readInt(sc, "Quantum base (>=1): ", 4, 1, Integer.MAX_VALUE);

        // Fixo/Dinâmico
        cfg.dynamic = readBool(sc, "Quantum dinâmico? (s/n): ", false);

        System.out.println("\nResumo:");
        System.out.println("- Arquivo: " + cfg.filePath + " (padrão)");
        System.out.println("- Núcleos: " + cfg.numCores);
        System.out.println("- Tick: " + cfg.tick);
        System.out.println("- Context Switch: " + cfg.contextSwitchCost);
        System.out.println("- Quantum base: " + cfg.baseQuantum + (cfg.dynamic ? " (dinâmico)" : " (fixo)"));
        System.out.println("================================\n");

        return cfg;
    }

    // ------- helpers -------
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
