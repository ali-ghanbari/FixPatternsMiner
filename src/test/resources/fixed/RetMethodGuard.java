public class RetMethGuard {
    public static void main(String[] args) {
        final String[] c = args.clone();
        if (c == null) {
            return;
        }
        System.out.println(c.clone());
    }
}