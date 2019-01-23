import java.io.IOException;
import java.io.PrintWriter;

public class CaseRemoval {
    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(args[0])) {
            pw.println("Hello!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}