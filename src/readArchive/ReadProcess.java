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

                if (line.isEmpty()) continue;

                if (header) {
                    header = false;
                    continue;
                }
                // Separa pelos Pipes |
                String[] parts = line.split("\\|");

                if (parts.length < 7) continue;

                String id = parts[0].trim();
                int entryTime = Integer.parseInt(parts[1].trim());
                int executeTime = Integer.parseInt(parts[2].trim());
                int remainingQuantum = Integer.parseInt(parts[3].trim());
                String state = parts[4].trim();
                Boolean blocked = Boolean.parseBoolean(parts[5].trim());
                int blockedTime = Integer.parseInt(parts[6].trim());

                ProcessData p = new ProcessData(id, entryTime, executeTime, remainingQuantum, state, blocked, blockedTime );
                process.add(p);

            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        };
        return process;
    }
}
