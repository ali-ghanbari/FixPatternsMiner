import java.io.PrintWriter;

public class CaseRemoval {
    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(args[0])) {
            pw.println("Hello!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}