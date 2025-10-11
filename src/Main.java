import java.util.List;
import readArchive.ReadProcess;
import entities.ProcessData;

public class Main {
    public static void main(String[] args) {
        List<ProcessData> list = ReadProcess.readProcess("src/readArchive/processos.txt");

        for (ProcessData p: list){
            System.out.println(p);
        }
    }
}
