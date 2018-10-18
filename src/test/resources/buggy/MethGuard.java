public class MethGuard {
    public static void main(String[] args) {
        final String[] c = args.clone();
        System.out.println(c.clone());
    }
}