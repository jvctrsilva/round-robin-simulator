package readArchive;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import entities.ProcessData;

public class ReadProcess {

    public static List <ProcessData> readProcess(String pathArchive) {
        List<ProcessData> process = new ArrayList<>();

        try (BufferedReader archive = Files.newBufferedReader(Paths.get(pathArchive))) {
            String line;
            boolean header = true;

            while ((line = archive.readLine()) != null) {
                line = line.trim();


                System.out.println("Linha lida: " + line);
                if (line.isEmpty()) continue;

                if (header) {
                    header = false;
                    continue;
                }
                // Separa pelos Pipes |
                String[] parts = line.split("\\|");

                if (parts.length < 6) continue;
                System.out.println("parts");

                String id = parts[0].trim();
                int timeEnter = Integer.parseInt(parts[1].trim());
                int timeExecute = Integer.parseInt(parts[2].trim());
                int timeRemaining = Integer.parseInt(parts[3].trim());
                int quantumRemaining = Integer.parseInt(parts[4].trim());
                String state = parts[5].trim();

                ProcessData p = new ProcessData(id, timeEnter, timeExecute, timeRemaining, quantumRemaining, state);
                process.add(p);
                System.out.println("Processo adicionado: " + p);


            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
        System.out.println("Processos lidos: " + process.size());
        return process;
    }
}
