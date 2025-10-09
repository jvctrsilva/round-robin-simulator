import java.util.List;
import readArchive.ReadProcess;
import entities.ProcessData;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        List<ProcessData> list = ReadProcess.readProcess("src/readArchive/processos.txt");

        for (ProcessData p: list){
            System.out.println(p);
        }
    }
}
